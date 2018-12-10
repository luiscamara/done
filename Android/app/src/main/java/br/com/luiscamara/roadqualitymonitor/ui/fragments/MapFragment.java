package br.com.luiscamara.roadqualitymonitor.ui.fragments;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Point;
import android.location.Location;
import android.os.Bundle;
import android.app.Fragment;
import android.os.Handler;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;

import com.google.android.gms.location.ActivityTransition;
import com.google.android.gms.location.DetectedActivity;

import org.osmdroid.api.IGeoPoint;
import org.osmdroid.api.IMapController;
import org.osmdroid.config.Configuration;
import org.osmdroid.events.DelayedMapListener;
import org.osmdroid.events.MapListener;
import org.osmdroid.events.ScrollEvent;
import org.osmdroid.events.ZoomEvent;
import org.osmdroid.tileprovider.tilesource.TileSourceFactory;
import org.osmdroid.util.BoundingBox;
import org.osmdroid.util.GeoPoint;
import org.osmdroid.util.TileSystem;
import org.osmdroid.views.MapView;
import org.osmdroid.views.Projection;
import org.osmdroid.views.overlay.FolderOverlay;
import org.osmdroid.views.overlay.Marker;
import org.osmdroid.views.overlay.Polyline;
import org.osmdroid.views.overlay.infowindow.BasicInfoWindow;

import java.util.ArrayList;
import java.util.List;

import br.com.luiscamara.roadqualitymonitor.data.models.ClassifiedTrack;
import br.com.luiscamara.roadqualitymonitor.data.models.MapExtent;
import br.com.luiscamara.roadqualitymonitor.data.models.Track;
import br.com.luiscamara.roadqualitymonitor.data.models.TrackQuality;
import br.com.luiscamara.roadqualitymonitor.network.services.RewardService;
import br.com.luiscamara.roadqualitymonitor.util.Constants;
import br.com.luiscamara.roadqualitymonitor.R;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.jackson.JacksonConverterFactory;

public class MapFragment extends Fragment {
    private BroadcastReceiver broadcastReceiver;
    private RewardService rewardService;
    private MapView mapView;
    private ImageButton startButton;
    private ImageButton locationLockButton;
    private SharedPreferences sharedPreferences;
    private Marker locationMarker;
    private FolderOverlay tracksOverlay;
    private double curLat, curLong;
    private int lastScrollX = -1, lastScrollY = -1;
    private long lastCameraUpdate = 0;
    private List<Track> submittedTracks;
    private boolean isReadingEnabled = false;
    private boolean runningMarkerAnimation = false;

    public static MapFragment newInstance() {
        MapFragment fragment = new MapFragment();
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        sharedPreferences = getContext().getSharedPreferences(getString(R.string.sharedprefs), Context.MODE_PRIVATE);
        Context ctx = getContext();
        Configuration.getInstance().load(ctx, PreferenceManager.getDefaultSharedPreferences(ctx));

        submittedTracks = new ArrayList<>();

        broadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                boolean isAutomaticDataAcquire = sharedPreferences.getBoolean(getString(R.string.automaticDataAcquire), true);
                boolean isDebugModeEnabled = sharedPreferences.getBoolean(getString(R.string.debugMode), false);

                if(intent.getAction().equals(Constants.BROADCAST_CURRENT_LOCATION)) {
                    double latitude = intent.getDoubleExtra("latitude", 0);
                    double longitude = intent.getDoubleExtra("longitude", 0);
                    float bearing = intent.getFloatExtra("bearing", 0);
                    locationMarker.setRotation(bearing);
                    animateMarkerPosition(latitude, longitude);
                }

                if (intent.getAction().equals(Constants.BROADCAST_DETECTED_ACTIVITY) && isAutomaticDataAcquire) {
                    int type = intent.getIntExtra("type", -1);
                    int enter = intent.getIntExtra("enter", 0);

                    // If user is in a vehicle, we should start reading sensors
                    // If he changes activity, we need to respond by disabling sensors
                    if (type == DetectedActivity.IN_VEHICLE) {
                        if (enter == ActivityTransition.ACTIVITY_TRANSITION_ENTER) {
                            setCarNavigationIcon();
                        } else {
                            setDefaultNavigationIcon();
                        }
                    }
                }

                if(intent.getAction().equals(Constants.BROADCAST_READINGS_STATUS_REPLY)) {
                    isReadingEnabled = intent.getBooleanExtra("isInitialized", false);
                    changeStartButtonIcon();
                    changeNavigationIcon();
                }

                if(isDebugModeEnabled) {
                    if (intent.getAction().equals(Constants.BROADCAST_SUBMITTED_TRACK)) {
                        Track t = (Track) intent.getSerializableExtra("track");
                        submittedTracks.add(t);
                    }

                    if (intent.getAction().equals(Constants.BROADCAST_TRACK_SENT)) {
                        Track track = (Track) intent.getSerializableExtra("track");
                        Track target = null;
                        for(Track t : submittedTracks) {
                            if(t.getUserID().equals(track.getUserID()) && t.getNumExecution().equals(track.getNumExecution()) && t.getNumSequence().equals(track.getNumSequence())) {
                                target = t;
                                break;
                            }
                        }

                        if(target != null)
                            submittedTracks.remove(target);
                    }
                }
            }
        };

        Retrofit retrofit = new Retrofit.Builder().baseUrl("http://onaweb.homeip.net/").addConverterFactory(JacksonConverterFactory.create()).build();
        rewardService = retrofit.create(RewardService.class);
    }

    private void changeStartButtonIcon() {
        if(isReadingEnabled) {
            startButton.setImageResource(R.drawable.ic_stop_black_24dp);
        } else {
            startButton.setImageResource(R.drawable.ic_play_circle_outline_black_24dp);
        }

        startButton.setActivated(true);
    }

    private void changeNavigationIcon() {
        if(isReadingEnabled) {
            setCarNavigationIcon();
        } else {
            setDefaultNavigationIcon();
        }
    }

    private void changeStartButtonVisibility() {
        boolean isAutomaticDataAcquire = sharedPreferences.getBoolean(getString(R.string.automaticDataAcquire), true);

        if(isAutomaticDataAcquire) {
            startButton.setVisibility(View.GONE);
        } else {
            startButton.setVisibility(View.VISIBLE);
        }
    }

    private void changeLocationButtonVisibility() {
        boolean isLockedToPosition = sharedPreferences.getBoolean(getString(R.string.isLockedToPosition), true);

        if(isLockedToPosition) {
            locationLockButton.setVisibility(View.GONE);
        } else {
            locationLockButton.setVisibility(View.VISIBLE);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.map_fragment, container, false);

        mapView = view.findViewById(R.id.mapView);
        startButton = view.findViewById(R.id.startButton);
        locationLockButton = view.findViewById(R.id.locationLockButton);

        startButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Send a broadcast asking for reading start
                Intent intent = new Intent(Constants.BROADCAST_INIT_READINGS);
                intent.putExtra("start", !isReadingEnabled);
                LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
                startButton.setActivated(false);
            }
        });

        locationLockButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                animateCameraPosition(locationMarker.getPosition());
                double centerLat = mapView.getMapCenter().getLatitude();
                double centerLong = mapView.getMapCenter().getLongitude();
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean(getString(R.string.isLockedToPosition), true);
                editor.putFloat(getString(R.string.map_position_X), (float)centerLat);
                editor.putFloat(getString(R.string.map_position_Y), (float)centerLong);
                editor.commit();
                changeLocationButtonVisibility();
            }
        });

        initializeMap();

        return view;
    }

    @Override
    public void onStart() {
        super.onStart();

        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.BROADCAST_DETECTED_ACTIVITY);
        filter.addAction(Constants.BROADCAST_CURRENT_LOCATION);
        filter.addAction(Constants.BROADCAST_SUBMITTED_TRACK);
        filter.addAction(Constants.BROADCAST_TRACK_SENT);
        filter.addAction(Constants.BROADCAST_READINGS_STATUS_REPLY);
        LocalBroadcastManager.getInstance(getContext()).registerReceiver(broadcastReceiver, filter);

        Intent intent = new Intent(Constants.BROADCAST_READINGS_STATUS_REQUEST);
        LocalBroadcastManager.getInstance(getContext()).sendBroadcast(intent);
    }

    @Override
    public void onResume() {
        super.onResume();
        mapView.onResume();
        changeStartButtonVisibility();
        changeStartButtonIcon();
        changeNavigationIcon();
        lastCameraUpdate = 0;
        lastScrollX = -1;
        lastScrollY = -1;
        getClassifiedTracks(getMapExtent());
    }

    @Override
    public void onPause() {
        super.onPause();
        mapView.onPause();
    }

    @Override
    public void onStop() {
        super.onStop();

        LocalBroadcastManager.getInstance(getContext()).unregisterReceiver(broadcastReceiver);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mapView.onDetach();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onLowMemory() {
        super.onLowMemory();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
    }

    private void initializeMap() {
        mapView.setTileSource(TileSourceFactory.MAPNIK);
        mapView.setBuiltInZoomControls(false);
        mapView.setMultiTouchControls(true);
        mapView.setHorizontalMapRepetitionEnabled(false);
        mapView.setVerticalMapRepetitionEnabled(false);
        mapView.setScrollableAreaLimitLatitude(TileSystem.MaxLatitude, -TileSystem.MaxLatitude, 0);
        mapView.setScrollableAreaLimitLongitude(-TileSystem.MaxLongitude, TileSystem.MaxLongitude, 0);

        mapView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View view, MotionEvent motionEvent) {
                locationLockButton.setVisibility(View.VISIBLE);

                // Save unlocked state and save position
                double centerLat = mapView.getMapCenter().getLatitude();
                double centerLong = mapView.getMapCenter().getLongitude();
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putBoolean(getString(R.string.isLockedToPosition), false);
                editor.putFloat(getString(R.string.map_position_X), (float) centerLat);
                editor.putFloat(getString(R.string.map_position_Y), (float) centerLong);
                editor.commit();

                return false;
            }
        });

        mapView.addMapListener(new DelayedMapListener(new MapListener() {
            @Override
            public boolean onScroll(ScrollEvent event) {
                if(getActivity() == null)
                    return false;

                boolean isLockedToPosition = sharedPreferences.getBoolean(getString(R.string.isLockedToPosition), true);
                if(isLockedToPosition)
                    return false;

                if(lastScrollX == -1 || lastScrollY == -1) {
                    lastScrollX = event.getX();
                    lastScrollY = event.getY();
                    return false;
                }

                // Save current map position
                double centerLat = event.getSource().getMapCenter().getLatitude();
                double centerLong = event.getSource().getMapCenter().getLongitude();
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putFloat(getString(R.string.map_position_X), (float)centerLat);
                editor.putFloat(getString(R.string.map_position_Y), (float)centerLong);
                editor.commit();

                // Check if has moved enough to reload track information
                int x = event.getX();
                int y = event.getY();
                int deltaX = x - lastScrollX;
                int deltaY = y - lastScrollY;
                double magnitude = Math.sqrt(Math.pow(deltaX, 2) + Math.pow(deltaY, 2));
                if(magnitude > 10) {
                    MapExtent extent = getMapExtent();
                    getClassifiedTracks(extent);
                }

                lastScrollX = event.getX();
                lastScrollY = event.getY();
                return false;
            }

            @Override
            public boolean onZoom(ZoomEvent event) {
                // To correct bug when orientation changes and activity does not exist yet
                // when "getString(R.string.map_zoom)" is called
                if(getActivity() == null)
                    return false;

                // Save current zoom level on shared preferences
                SharedPreferences.Editor editor = sharedPreferences.edit();
                editor.putString(getString(R.string.map_zoom), String.valueOf(mapView.getZoomLevelDouble()));
                editor.commit();

                MapExtent extent = getMapExtent();
                getClassifiedTracks(extent);
                return false;
            }
        }, 200));
        mapView.setMinZoomLevel(4.0);
        mapView.setMaxZoomLevel(21.0);

        changeLocationButtonVisibility();

        // Reset overlays
        mapView.getOverlayManager().clear();

        // Create location marker
        locationMarker = new Marker(mapView);
        locationMarker.setAnchor(Marker.ANCHOR_CENTER, Marker.ANCHOR_BOTTOM);
        setDefaultNavigationIcon();
        locationMarker.setFlat(true);
        curLat = Double.parseDouble(sharedPreferences.getString(getString(R.string.current_latitude), "0"));
        curLong = Double.parseDouble(sharedPreferences.getString(getString(R.string.current_longitude), "0"));
        locationMarker.setPosition(new GeoPoint(curLat, curLong));
        mapView.getOverlayManager().add(locationMarker);

        // Set map camera zoom and position
        IMapController mapController = mapView.getController();
        double zoom = Double.parseDouble(sharedPreferences.getString(getString(R.string.map_zoom), "19"));
        mapController.setZoom(zoom);

        boolean isLockedToPosition = sharedPreferences.getBoolean(getString(R.string.isLockedToPosition), true);
        if(!isLockedToPosition) {
            double centerLat = sharedPreferences.getFloat(getString(R.string.map_position_X), 0);
            double centerLong = sharedPreferences.getFloat(getString(R.string.map_position_Y), 0);
            mapController.setCenter(new GeoPoint(centerLat, centerLong));
        } else {
            mapController.setCenter(locationMarker.getPosition());
        }

        // Invalidate map view
        mapView.invalidate();

        // Load tracks
        getClassifiedTracks(getMapExtent());
    }

    private void animateCameraPosition(GeoPoint point) {
        IMapController mapController = mapView.getController();
        mapController.animateTo(point);
        getClassifiedTracks(getMapExtent());
    }

    private void animateMarkerPosition(double latitude, double longitude) {
        GeoPoint point = new GeoPoint(latitude, longitude);

        if(locationMarker != null && !runningMarkerAnimation) {
            if(lastCameraUpdate == 0) {
                locationMarker.setPosition(point);
            } else {
                //long deltaTime = System.currentTimeMillis() - lastCameraUpdate;
                animateMarker(locationMarker, point);
            }
        }

        boolean isLockedToPosition = sharedPreferences.getBoolean(getString(R.string.isLockedToPosition), true);
        if(isLockedToPosition) {
            animateCameraPosition(point);
        }

        lastCameraUpdate = System.currentTimeMillis();
    }

    private void setDefaultNavigationIcon() {
        locationMarker.setIcon(getResources().getDrawable(R.drawable.ic_location));
    }

    private void setCarNavigationIcon() {
        locationMarker.setIcon(getResources().getDrawable(R.drawable.ic_moving));
    }

    private MapExtent getMapExtent() {
        BoundingBox boundingBox = mapView.getBoundingBox();
        MapExtent extent = new MapExtent(boundingBox.getLatNorth(), boundingBox.getLatSouth(), boundingBox.getLonEast(), boundingBox.getLonWest());
        return extent;
    }

    private void getClassifiedTracks(MapExtent extent) {
        Call call = rewardService.query(extent);
        call.enqueue(new Callback<Iterable<ClassifiedTrack>>() {
            @Override
            public void onResponse(Call<Iterable<ClassifiedTrack>> call, Response<Iterable<ClassifiedTrack>> response) {
                if(getActivity() == null)
                    return;

                if(response.isSuccessful()) {
                    Iterable<ClassifiedTrack> tracks = response.body();
                    DrawTracks(tracks);
                } else {
                    Log.e("MapFragment", "Failed to get classified tracks!");
                }
            }

            @Override
            public void onFailure(Call<Iterable<ClassifiedTrack>> call, Throwable t) {
                if(getActivity() == null)
                    return;

                Log.e("MapFragment", "Failed to get classified tracks!");
                Log.e("MapFragment", t.getMessage());
                call.clone().enqueue(this);
            }
        });
    }

    private void DrawTracks(Iterable<ClassifiedTrack> tracks) {
        mapView.getOverlayManager().remove(tracksOverlay);
        tracksOverlay = new FolderOverlay();

        for(ClassifiedTrack ct : tracks) {
            if(ct.quality == TrackQuality.UNKNOWN)
                continue;

            List<GeoPoint> points = new ArrayList();
            points.add(new GeoPoint(ct.startPosition.getX(), ct.startPosition.getY()));
            points.add(new GeoPoint(ct.endPosition.getX(), ct.endPosition.getY()));
            Polyline line = new Polyline();
            line.setPoints(points);
            line.setInfoWindow(new BasicInfoWindow(R.layout.bonuspack_bubble, mapView));
            line.setTitle("Trecho");
            line.setSubDescription("Velocidade: " + ct.averageVelocity + "\n" + "eIRI: " + ct.eIRI);
            if(ct.quality  == TrackQuality.EXCELLENT)
                line.setColor(Color.GREEN);
            if(ct.quality  == TrackQuality.GOOD)
                line.setColor(Color.rgb(0, 128, 0));
            if(ct.quality  == TrackQuality.AVERAGE)
                line.setColor(Color.YELLOW);
            if(ct.quality  == TrackQuality.BAD)
                line.setColor(Color.rgb(200, 100, 0));
            if(ct.quality  == TrackQuality.TERRIBLE)
                line.setColor(Color.RED);

            tracksOverlay.add(line);
        }

        boolean isDebugModeEnabled = sharedPreferences.getBoolean(getString(R.string.debugMode), false);
        if(isDebugModeEnabled) {
            for (Track t : submittedTracks) {
                List<GeoPoint> points = new ArrayList();
                points.add(new GeoPoint(t.getStartPosition().getX(), t.getStartPosition().getY()));
                points.add(new GeoPoint(t.getEndPosition().getX(), t.getEndPosition().getY()));
                Polyline line = new Polyline();
                line.setPoints(points);
                line.setInfoWindow(new BasicInfoWindow(R.layout.bonuspack_bubble, mapView));
                line.setTitle("Trecho submetido para avaliação");
                line.setSubDescription("Velocidade: " + t.getAverageVelocity());
                line.setColor(Color.GRAY);

                tracksOverlay.add(line);
            }
        }

        mapView.getOverlayManager().add(tracksOverlay);
        mapView.getOverlayManager().remove(locationMarker);
        mapView.getOverlayManager().add(locationMarker);
    }

    public void animateMarker(final Marker marker, final GeoPoint toPosition) {
        final Handler handler = new Handler();
        final long start = SystemClock.uptimeMillis();
        Projection proj = mapView.getProjection();
        Point startPoint = proj.toPixels(marker.getPosition(), null);
        final IGeoPoint startGeoPoint = proj.fromPixels(startPoint.x, startPoint.y);
        final long duration = 500;
        final Interpolator interpolator = new LinearInterpolator();
        handler.post(new Runnable() {
            @Override
            public void run() {
                runningMarkerAnimation = true;

                long elapsed = SystemClock.uptimeMillis() - start;
                float t = interpolator.getInterpolation((float) elapsed / duration);
                double lng = t * toPosition.getLongitude() + (1 - t) * startGeoPoint.getLongitude();
                double lat = t * toPosition.getLatitude() + (1 - t) * startGeoPoint.getLatitude();
                marker.setPosition(new GeoPoint(lat, lng));
                if (t < 1.0) {
                    handler.postDelayed(this, 15);
                }
                mapView.postInvalidate();

                runningMarkerAnimation = false;
            }
        });
    }
}
