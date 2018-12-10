package br.com.luiscamara.roadqualitymonitor.services;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.Settings;
import android.support.annotation.Nullable;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.location.ActivityRecognition;
import com.google.android.gms.location.ActivityTransition;
import com.google.android.gms.location.ActivityTransitionRequest;
import com.google.android.gms.location.DetectedActivity;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.PrecisionModel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import br.com.luiscamara.roadqualitymonitor.MainActivity;
import br.com.luiscamara.roadqualitymonitor.R;
import br.com.luiscamara.roadqualitymonitor.data.TrackDatabase;
import br.com.luiscamara.roadqualitymonitor.data.models.Device;
import br.com.luiscamara.roadqualitymonitor.data.models.Track;
import br.com.luiscamara.roadqualitymonitor.data.models.VerticalAccelerationReading;
import br.com.luiscamara.roadqualitymonitor.data.services.TrackService;
import br.com.luiscamara.roadqualitymonitor.data.tasks.ListTracksAsync;
import br.com.luiscamara.roadqualitymonitor.data.tasks.OnTaskCompleted;
import br.com.luiscamara.roadqualitymonitor.network.services.CollaborationService;
import br.com.luiscamara.roadqualitymonitor.util.Constants;
import br.com.luiscamara.roadqualitymonitor.util.RetryCallback;
import retrofit2.Call;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

import static android.net.ConnectivityManager.TYPE_WIFI;
import static br.com.luiscamara.roadqualitymonitor.App.CHANNEL_ID;

public class BackgroundService extends Service implements SensorEventListener, LocationListener, OnTaskCompleted {
    private static final String TAG = BackgroundService.class.getSimpleName();
    private static final GeometryFactory geometryFactory = new GeometryFactory(new PrecisionModel(), 3857);
    private static final int SPEED_THRESHOLD = 20;
    private static final int TRACK_MINIMUM_SIZE = 10;
    private static final int BUFFER_SEND_INTERVAL_IN_SECS = 5;
    private SharedPreferences sharedPreferences;
    private TrackService trackService;
    private CollaborationService collaborationService;
    private Intent mIntentService;
    private PendingIntent mPendingIntent;
    private LocationManager mLocationManager = null;
    private SensorManager mSensorManager = null;
    private Sensor mLinearAcceleration;
    private Sensor mGravityNormalized;
    private Sensor mAccelerometer;
    private BroadcastReceiver broadcastReceiver;
    private boolean usePrimarySensors = true;
    private boolean canColaborate = true;
    private boolean hasGNSS = true;
    private boolean readingStarted = false;
    private boolean shouldSendVerticalAccelerationData = true;
    private boolean startedGNSSReading = false;
    private boolean isAutomaticDataAcquire = true;
    private float[] gravity = new float[3];
    private float[] gravityNormalized = new float[3];
    private Track currentTrack = null;
    private Location startLocation = null;
    private Long lastAccelerometerReadingTime = 0l;
    private PowerManager.WakeLock wakeLock;
    private Context serviceContext;

    // DEBUG INFORMATION
    private Long numExecutions;
    private Long numSequence = 1l;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    public BackgroundService() {
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, 0);
        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Road Quality Monitor está executando")
                .setContentText("Para encerrar acesse o aplicativo")
                .setSmallIcon(R.drawable.ic_road)
                .setContentIntent(pendingIntent)
                .build();
        startForeground(1, notification);
        return START_STICKY;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "Loading background service!");

        serviceContext = getApplicationContext().createDeviceProtectedStorageContext();
        sharedPreferences = serviceContext.getSharedPreferences(getString(R.string.sharedprefs), Context.MODE_PRIVATE);
        isAutomaticDataAcquire = sharedPreferences.getBoolean(getString(R.string.automaticDataAcquire), true);

        initializeGNSS();
        initializeSensors();
        initializeActivityRecognition();
        initializeRetrofit();

        numExecutions = sharedPreferences.getLong(getString(R.string.number_executions), 1);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putLong(getString(R.string.number_executions), numExecutions + 1);
        editor.apply();

        // Start GNSS readings
        startGNSS();

        // Check user activity detection
        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals(Constants.BROADCAST_DETECTED_ACTIVITY) && isAutomaticDataAcquire) {
                    Log.d(TAG, "Broadcast de atividade detectada recebido!");
                    int type = intent.getIntExtra("type", -1);
                    int enter = intent.getIntExtra("enter", 0);

                    // If user is in a vehicle, we should start reading sensors
                    // If he changes activity, we need to respond by disabling sensors
                    if (type == DetectedActivity.IN_VEHICLE) {
                        if (enter == ActivityTransition.ACTIVITY_TRANSITION_ENTER) {
                            startReadings();
                        } else {
                            stopReadings();
                        }
                    }
                }

                if(intent.getAction().equals(Constants.BROADCAST_INIT_READINGS)) {
                    boolean start = intent.getBooleanExtra("start", false);
                    Log.d(TAG, "Broadcast de start manual recebido!");
                    if(start) {
                        startReadings();
                    } else {
                        stopReadings();
                    }
                }

                if(intent.getAction().equals(Constants.BROADCAST_AUTOMATIC_READINGS_ENABLE)) {
                    boolean enabled = intent.getBooleanExtra("enabled", false);
                    isAutomaticDataAcquire = enabled;
                    stopReadings();
                }

                if(intent.getAction().equals(Constants.BROADCAST_READINGS_STATUS_REQUEST)) {
                    Intent newIntent = new Intent(Constants.BROADCAST_READINGS_STATUS_REPLY);
                    newIntent.putExtra("isInitialized", readingStarted);
                    LocalBroadcastManager.getInstance(context).sendBroadcast(newIntent);
                }
            }
        };

        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.BROADCAST_DETECTED_ACTIVITY);
        filter.addAction(Constants.BROADCAST_INIT_READINGS);
        filter.addAction(Constants.BROADCAST_AUTOMATIC_READINGS_ENABLE);
        filter.addAction(Constants.BROADCAST_READINGS_STATUS_REQUEST);
        LocalBroadcastManager.getInstance(this).registerReceiver(broadcastReceiver, filter);

        if(canColaborate) {
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
            initializeDB();
            registerDevice();
            scheduleSendBufferData();
        }
    }

    private void initializeDB() {
        trackService = new TrackService(getApplicationContext());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        Task<Void> task = ActivityRecognition.getClient(this).removeActivityTransitionUpdates(mPendingIntent);

        task.addOnSuccessListener(
                result -> mPendingIntent.cancel());

        task.addOnFailureListener(
                e -> Log.e(TAG, e.getMessage()));

        stopGNSS();

        currentTrack = null;
        mSensorManager.unregisterListener(this);

        LocalBroadcastManager.getInstance(this).unregisterReceiver(broadcastReceiver);
    }

    private void initializeGNSS() {
        if(mLocationManager == null) {
            mLocationManager = (LocationManager)serviceContext.getSystemService(Context.LOCATION_SERVICE);
        }

        // Check if GPS provider is available
        if(mLocationManager.getProvider(LocationManager.GPS_PROVIDER) == null) {
            Log.e(TAG, "Device cannot colaborate or provide map information!");
            canColaborate = false;
            hasGNSS = false;

            // Stop sensor service
            stopSelf();
        }
    }

    private void initializeSensors() {
        if(!canColaborate)
            return;

        if(mSensorManager == null) {
            mSensorManager = (SensorManager) getApplicationContext().getSystemService(Context.SENSOR_SERVICE);
        }

        // Initialize primary sensors
        mLinearAcceleration = mSensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION);
        mGravityNormalized = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);

        // Check if primary sensors are available
        if(mLinearAcceleration == null || mGravityNormalized == null) {
            usePrimarySensors = false;

            mAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

            if (mAccelerometer == null) {
                Log.e(TAG, "Device cannot colaborate!");
                canColaborate = false;
            }
        }
    }

    private void initializeActivityRecognition() {
        if(!canColaborate)
            return;

        List<ActivityTransition> transitions = new ArrayList<>();
        transitions.add(
                new ActivityTransition.Builder()
                        .setActivityType(DetectedActivity.IN_VEHICLE)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_ENTER)
                        .build());
        transitions.add(
                new ActivityTransition.Builder()
                        .setActivityType(DetectedActivity.IN_VEHICLE)
                        .setActivityTransition(ActivityTransition.ACTIVITY_TRANSITION_EXIT)
                        .build());

        ActivityTransitionRequest request = new ActivityTransitionRequest(transitions);

        mIntentService = new Intent(this, DetectedActivitiesIntentService.class);
        mPendingIntent = PendingIntent.getService(this, 1, mIntentService, PendingIntent.FLAG_UPDATE_CURRENT);

        Task<Void> task = ActivityRecognition.getClient(this).requestActivityTransitionUpdates(request, mPendingIntent);

        task.addOnSuccessListener(aVoid -> Log.d(TAG, "Successfully registered for activity recognition updates!"));
        task.addOnFailureListener(e -> Log.e(TAG, e.getMessage()));
    }

    private void initializeRetrofit() {
        if(!canColaborate)
            return;

        Retrofit retrofit = new Retrofit.Builder().baseUrl("http://onaweb.homeip.net/").addConverterFactory(JacksonConverterFactory.create()).build();
        collaborationService = retrofit.create(CollaborationService.class);
    }

    private void registerDevice() {
        Device d = new Device();
        d.userID = Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID);
        d.userDevice = Build.MODEL;

        Call call = collaborationService.registerDevice(d);
        call.enqueue(new RetryCallback<Device>() {
            @Override
            public void onFinalResponse(Call<Device> call, Response<Device> response) {
                if(response.isSuccessful()) {
                    SharedPreferences.Editor editor = sharedPreferences.edit();
                    editor.putBoolean(getString(R.string.device_registered), true);
                    editor.apply();
                    Log.i(TAG,"Device registered!");
                } else {
                    Log.i(TAG, "Failed to register device!");
                }
            }

            @Override
            public void onFailedAfterRetry(Throwable t) {
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean(getString(R.string.device_registered), false);
                editor.apply();
                Log.e(TAG, "Failed to register device!");
                Log.e(TAG, t.getMessage());
            }
        });
    }

    private void scheduleSendBufferData() {
        ListTracksAsync listTracks = new ListTracksAsync(this);
        listTracks.execute(getApplicationContext());
    }

    @Override
    public void onTaskCompleted(List<Track> pendingTracks) {
        // TODO: Include option for mobile connection if specified by the user
        // If WIFI is not available, bail out
        //if(!isWifiConnected()) {
        //scheduleSendBufferData();
        //return;
        //}

        if(isOnline()) {
            for (final Track t : pendingTracks) {
                Call call = collaborationService.collaborate(t);
                call.enqueue(new RetryCallback<Track>() {
                    @Override
                    public void onFinalResponse(final Call<Track> call, Response<Track> response) {
                        if (response.isSuccessful()) {
                            Log.i(TAG, "Track enviada!");
                            boolean isDebugModeEnabled = sharedPreferences.getBoolean(getString(R.string.debugMode), false);
                            if (isDebugModeEnabled) {
                                Intent intent = new Intent(Constants.BROADCAST_TRACK_SENT);
                                intent.putExtra("track", t);
                                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(intent);
                            }
                        } else {
                            Log.i(TAG, "Envio da track teve resposta mas não foi bem sucedida!");
                            //new Thread(() -> trackService.insert(t)).start();
                            //call.clone().enqueue(this);
                        }
                    }

                    @Override
                    public void onFailedAfterRetry(Throwable error) {
                        Log.e(TAG, "Falha ao enviar track!");
                        Log.e(TAG, error.getMessage());
                        t.setId(null);
                        for(VerticalAccelerationReading va : t.getVerticalAccelerationReadings()) {
                            va.setId(null);
                        }

                        new Thread(() -> trackService.insert(t)).start();
                    }
                });

                new Thread(() -> trackService.remove(t)).start();
            }
        }

        Handler handler = new Handler();
        handler.postDelayed(() -> scheduleSendBufferData(), BUFFER_SEND_INTERVAL_IN_SECS * 1000);
    }

    public boolean isWifiConnected() {
        ConnectivityManager cm = (ConnectivityManager)this.getSystemService(Context.CONNECTIVITY_SERVICE);

        return (cm != null) && (cm.getActiveNetworkInfo() != null) &&
                (cm.getActiveNetworkInfo().getType() == TYPE_WIFI);
    }

    public boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }

    private void startGNSS() {
        try {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, this);

            Location lastLocation = mLocationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

            // Save last location on shared preferences
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString(getString(R.string.current_latitude), String.valueOf(lastLocation.getLatitude()));
            editor.putString(getString(R.string.current_longitude), String.valueOf(lastLocation.getLongitude()));
            editor.apply();

            broadcastCurrentLocation(lastLocation);
        } catch(SecurityException ex) {
            Log.e(TAG, "Device is not authorized to get GNSS information", ex);
        }
    }

    private void stopGNSS() {
        mLocationManager.removeUpdates(this);
        startedGNSSReading = false;
    }

    private void startReadings() {
        if(!readingStarted) {
            if(usePrimarySensors) {
                mSensorManager.registerListener(this, mLinearAcceleration, SensorManager.SENSOR_DELAY_FASTEST);
                mSensorManager.registerListener(this, mGravityNormalized, SensorManager.SENSOR_DELAY_FASTEST);
            } else {
                mSensorManager.registerListener(this, mAccelerometer, SensorManager.SENSOR_DELAY_FASTEST);
            }

            wakeLock.acquire();
            readingStarted = true;

            // Send a broadcast with reading started status
            Intent intent = new Intent(Constants.BROADCAST_READINGS_STATUS_REPLY);
            intent.putExtra("isInitialized", readingStarted);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        }
    }

    private void stopReadings() {
        mSensorManager.unregisterListener(this);
        if(wakeLock.isHeld())
            wakeLock.release();
        readingStarted = false;

        // Send a broadcast with reading started status
        Intent intent = new Intent(Constants.BROADCAST_READINGS_STATUS_REPLY);
        intent.putExtra("isInitialized", readingStarted);
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

    @Override
    public void onSensorChanged(SensorEvent sensorEvent) {
        switch(sensorEvent.sensor.getType()) {
            // PRIMARY SENSORS
            case Sensor.TYPE_LINEAR_ACCELERATION:
                ReadLinearAcceleration(sensorEvent);
                break;
            case Sensor.TYPE_GRAVITY:
                ReadGravity(sensorEvent);
                break;

            // SECONDARY SENSORS
            case Sensor.TYPE_ACCELEROMETER:
                ReadAccelerometerData(sensorEvent);
                break;

            // UNEXPECTED DATA
            default:
                Log.d(TAG, "Receiving unexpected sensor data from sensor " + sensorEvent.sensor.getType() + "!");
                break;
        }
    }

    private void ReadLinearAcceleration(SensorEvent event) {
        // Cannot continue unless gravityNormalized is set
        if(gravityNormalized[0] == 0 && gravityNormalized[1] == 0 && gravityNormalized[2] == 0)
            return;

        long currentAccelerometerReadingTime = System.nanoTime();
        long timeBetweenReadings;

        if(lastAccelerometerReadingTime == 0)
            timeBetweenReadings = 0;
        else
            timeBetweenReadings = currentAccelerometerReadingTime - lastAccelerometerReadingTime;

        float[] linearAcceleration = new float[3];
        for(int i = 0; i < 3; i++) {
            linearAcceleration[i] = event.values[i];
        }

        double verticalAcceleration = linearAcceleration[0] * gravityNormalized[0] +
                                      linearAcceleration[1] * gravityNormalized[1] +
                                      linearAcceleration[2] * gravityNormalized[2];

        if(startedGNSSReading && currentTrack != null) {
            VerticalAccelerationReading va = new VerticalAccelerationReading();
            va.setY((float)verticalAcceleration);
            va.setTimeSinceLastReading(timeBetweenReadings);
            currentTrack.getVerticalAccelerationReadings().add(va);
        }

        if(shouldSendVerticalAccelerationData) {
            // Send current vertical acceleration
            Intent intent = new Intent(Constants.BROADCAST_VERTICAL_ACCELERATION_DATA);
            intent.putExtra("va", (float)verticalAcceleration);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        }

        lastAccelerometerReadingTime = currentAccelerometerReadingTime;
    }

    private void ReadGravity(SensorEvent event) {
        for(int i = 0; i < 3; i++) {
            gravity[i] = event.values[i];
        }

        float gravityNormalizedMagnitude = (float)Math.sqrt(    Math.pow(gravity[0], 2) +
                                                                Math.pow(gravity[1], 2) +
                                                                Math.pow(gravity[2], 2)
                                                  );
        gravityNormalized[0] = gravity[0] / gravityNormalizedMagnitude;
        gravityNormalized[1] = gravity[1] / gravityNormalizedMagnitude;
        gravityNormalized[2] = gravity[2] / gravityNormalizedMagnitude;
    }

    private void ReadAccelerometerData(SensorEvent event) {
        long currentAccelerometerReadingTime = System.nanoTime();
        long timeBetweenReadings;

        if(lastAccelerometerReadingTime == 0)
            timeBetweenReadings = 0;
        else
            timeBetweenReadings = currentAccelerometerReadingTime - lastAccelerometerReadingTime;

        // Calculate gravityNormalized based on low pass filter
        float alpha = 0.8f;
        for(int i = 0; i < 3; i++) {
            gravity[i] = alpha * gravity[i] + (1 - alpha) * event.values[i];
        }

        float[] linearAcceleration = new float[3];
        for(int i = 0; i < 3; i++) {
            linearAcceleration[i] = event.values[i] - gravity[i];
        }

        float gravityNormalizedMagnitude = (float)Math.sqrt(    Math.pow(gravity[0], 2) +
                                                                Math.pow(gravity[1], 2) +
                                                                Math.pow(gravity[2], 2) );
        gravityNormalized[0] = gravity[0] / gravityNormalizedMagnitude;
        gravityNormalized[1] = gravity[1] / gravityNormalizedMagnitude;
        gravityNormalized[2] = gravity[2] / gravityNormalizedMagnitude;

        double verticalAcceleration = linearAcceleration[0] * gravityNormalized[0] +
                                      linearAcceleration[1] * gravityNormalized[1] +
                                      linearAcceleration[2] * gravityNormalized[2];

        if(startedGNSSReading && currentTrack != null) {
            VerticalAccelerationReading va = new VerticalAccelerationReading();
            va.setY((float)verticalAcceleration);
            va.setTimeSinceLastReading(timeBetweenReadings);
            currentTrack.getVerticalAccelerationReadings().add(va);
        }

        if(shouldSendVerticalAccelerationData) {
            // Send current vertical acceleration
            Intent intent = new Intent(Constants.BROADCAST_VERTICAL_ACCELERATION_DATA);
            intent.putExtra("va", verticalAcceleration);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        }

        lastAccelerometerReadingTime = currentAccelerometerReadingTime;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {
        Log.i(TAG, "Precisão dos dados do sensor " + sensor.getName() + " mudou para " + i + "!");
    }

    @Override
    public void onLocationChanged(Location location) {
        // Save current location on shared preferences
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putString(getString(R.string.current_latitude), String.valueOf(location.getLatitude()));
        editor.putString(getString(R.string.current_longitude), String.valueOf(location.getLongitude()));
        editor.apply();

        // Send a broadcast with current location
        broadcastCurrentLocation(location);

        // Do this part only if readings have started
        if(readingStarted) {
            // Register tracks
            // First GPS reading?
            if (!startedGNSSReading) {
                Log.i(TAG, "Iniciando coleta das tracks...");
                startedGNSSReading = true;
                // Start registering new track
                beginTrack(location);
            } else {
                if(endTrack(location))
                    beginTrack(location);
            }
        }
    }

    private void beginTrack(Location location) {
        Log.i(TAG, "Iniciando nova track...");
        currentTrack = new Track();
        currentTrack.setUserID(Settings.Secure.getString(getContentResolver(), Settings.Secure.ANDROID_ID));
        currentTrack.setNumExecution(numExecutions);
        currentTrack.setNumSequence(numSequence++);
        currentTrack.setStartPosition(geometryFactory.createPoint(new Coordinate(location.getLatitude(), location.getLongitude())));
        currentTrack.setStartTime(new Date(System.currentTimeMillis()));
        startLocation = location;
    }

    private boolean endTrack(Location location) {
        if(location.distanceTo(startLocation) < TRACK_MINIMUM_SIZE)
            return false;

        // Get elapsed time between initial track reading
        BigDecimal elapsedTime = new BigDecimal((System.currentTimeMillis() - currentTrack.getStartTime().getTime()) / 1000);
        if(elapsedTime.equals(BigDecimal.ZERO))
            return false;

        // Track average speed in KM/H
        BigDecimal distance = new BigDecimal(location.distanceTo(startLocation));
        BigDecimal trackAverageSpeed = distance.divide(elapsedTime, RoundingMode.HALF_UP).multiply(new BigDecimal(3.6f));
        if(trackAverageSpeed.floatValue() < SPEED_THRESHOLD) {
            Log.i(TAG,"Track coletada mas não cumpre os requisitos de velocidade!");
            return true;
        }

        currentTrack.setEndPosition(geometryFactory.createPoint(new Coordinate(location.getLatitude(), location.getLongitude())));
        currentTrack.setEndTime(new Date(System.currentTimeMillis()));
        currentTrack.setAverageVelocity(trackAverageSpeed.floatValue());

        // Send information to server
        trackService.insert(currentTrack);

        boolean isDebugModeEnabled = sharedPreferences.getBoolean(getString(R.string.debugMode), false);
        if(isDebugModeEnabled) {
            Intent intent = new Intent(Constants.BROADCAST_SUBMITTED_TRACK);
            intent.putExtra("track", currentTrack);
            LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
        }

        Log.i(TAG,"Track coletada!");
        return true;
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {
        switch(i) {
            case LocationProvider.OUT_OF_SERVICE:
                startedGNSSReading = false;
                currentTrack = null;
                Log.i(TAG, "GNSS fora de serviço!");
                break;
            case LocationProvider.TEMPORARILY_UNAVAILABLE:
                startedGNSSReading = false;
                currentTrack = null;
                Log.i(TAG, "GNSS indisponível temporariamente!");
                break;
            case LocationProvider.AVAILABLE:
                //Log.i(TAG, "GNSS disponível!");
                break;
            default:
                Log.i(TAG, "Status desconhecido para o GPS!");
                break;
        }
    }

    @Override
    public void onProviderEnabled(String s) {
        Log.i(TAG, "Provider para localização ligado!");
    }

    @Override
    public void onProviderDisabled(String s) {
        startedGNSSReading = false;
        currentTrack = null;
        Log.i(TAG, "Provider para localização desligado!");
    }

    public void showNotification(String title, String ticker) {
        NotificationManager mgr = (NotificationManager)this.getSystemService(Context.NOTIFICATION_SERVICE);
        NotificationCompat.Builder note = new NotificationCompat.Builder(this);
        note.setContentTitle(title);
        note.setTicker(ticker);
        note.setAutoCancel(true);
        // to set default sound/light/vibrate or all
        note.setDefaults(Notification.DEFAULT_ALL);
        // Icon to be set on Notification
        note.setSmallIcon(R.drawable.ic_launcher_background);
        // This pending intent will open after notification click
        PendingIntent pi = PendingIntent.getActivity(this, 0, new Intent(this, MainActivity.class), 0);
        // set pending intent to notification builder
        note.setContentIntent(pi);
        mgr.notify(101, note.build());
    }

    private void createNotification() {

    }

    private void broadcastCurrentLocation(Location location) {
        // Send a broadcast with current location
        Intent intent = new Intent(Constants.BROADCAST_CURRENT_LOCATION);
        intent.putExtra("latitude", location.getLatitude());
        intent.putExtra("longitude", location.getLongitude());
        intent.putExtra("bearing", location.getBearing());
        LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
    }

}
