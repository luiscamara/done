package br.com.luiscamara.roadqualitymonitor;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.ActivityCompat;
import android.app.Fragment;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.MenuItem;
import android.view.WindowManager;

import br.com.luiscamara.roadqualitymonitor.services.BackgroundService;
import br.com.luiscamara.roadqualitymonitor.ui.fragments.MapFragment;
import br.com.luiscamara.roadqualitymonitor.ui.fragments.OptionsFragment;
import br.com.luiscamara.roadqualitymonitor.ui.fragments.SensorsFragment;
import br.com.luiscamara.roadqualitymonitor.util.Constants;

public class MainActivity extends AppCompatActivity {
    private String TAG = MainActivity.class.getSimpleName();
    private boolean locationAuthorized = false;
    private boolean externalStorageAuthorized = false;
    private SharedPreferences settings;
    private int currentFragmentIndex = -1;
    private Intent backgroundServiceIntent = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        settings = getSharedPreferences("RQM", MODE_PRIVATE);

        // Keep screen on while app is loaded
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        askForPermissions();
        configFragments();
    }

    private void askForPermissions() {
        Log.d(TAG, "Asking for location tracking permission...");
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.ACCESS_FINE_LOCATION }, Constants.GNSS_PERMISSION);
        } else {
            Log.d(TAG, "Location tracking permission granted!");
            startBackgroundService();
        }

        Log.d(TAG, "Asking for external storage write permission...");
        boolean storagePermission = false;
        if(ActivityCompat.checkSelfPermission(this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[] { Manifest.permission.WRITE_EXTERNAL_STORAGE }, Constants.STORAGE_PERMISSION);
        } else {
            Log.d(TAG, "External storage write permission granted!");
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case Constants.GNSS_PERMISSION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "Location tracking permission granted!");
                    startBackgroundService();
                } else {
                    Log.d(TAG, "Location tracking permission denied! Can't collaborate without location tracking!");
                    locationAuthorized = false;
                }

                return;
            }

            case Constants.STORAGE_PERMISSION: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.d(TAG, "External storage write permission granted!");
                } else {
                    Log.e(TAG, "External storage write permission denied! Map component cannot work without cache!");
                    externalStorageAuthorized = false;
                }

                return;
            }
        }
    }

    private void configFragments() {
        BottomNavigationView bottomNavigationView = findViewById(R.id.menu);

        int selectedFragment = settings.getInt(getString(R.string.selected_fragment), 0);
        Fragment fragmentToLoad;
        switch(selectedFragment) {
            case 0:
                fragmentToLoad = MapFragment.newInstance();
                bottomNavigationView.setSelectedItemId(R.id.action_map);
                break;
            case 1:
                fragmentToLoad = SensorsFragment.newInstance();
                bottomNavigationView.setSelectedItemId(R.id.action_sensors);
                break;
            case 2:
                fragmentToLoad = OptionsFragment.newInstance();
                bottomNavigationView.setSelectedItemId(R.id.action_options);
                break;
            default:
                fragmentToLoad = MapFragment.newInstance();
                break;
        }

        getFragmentManager().beginTransaction().replace(R.id.fragmentContainer, fragmentToLoad).commit();
        currentFragmentIndex = selectedFragment;


        bottomNavigationView.setOnNavigationItemSelectedListener(new BottomNavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                Fragment selectedFragment = null;
                int fragmentIndex = -1;
                switch(item.getItemId()) {
                    case R.id.action_map: {
                        selectedFragment = MapFragment.newInstance();
                        saveCurrentFragment(0);
                        fragmentIndex = 0;
                        break;
                    }

                    case R.id.action_sensors: {
                        selectedFragment = SensorsFragment.newInstance();
                        saveCurrentFragment(1);
                        fragmentIndex = 1;
                        break;
                    }

                    case R.id.action_options: {
                        selectedFragment = OptionsFragment.newInstance();
                        saveCurrentFragment(2);
                        fragmentIndex = 2;
                        break;
                    }

                    case R.id.action_exit: {
                        if(backgroundServiceIntent != null)
                            stopService(backgroundServiceIntent);
                        System.exit(1);
                        break;
                    }
                }

                if(currentFragmentIndex != fragmentIndex) {
                    getFragmentManager().beginTransaction().replace(R.id.fragmentContainer, selectedFragment, String.valueOf(currentFragmentIndex)).commit();
                    currentFragmentIndex = fragmentIndex;
                }

                return true;
            }
        });
    }

    private void startBackgroundService() {
        Log.d(TAG, "Starting background service...");
        backgroundServiceIntent = new Intent(MainActivity.this, BackgroundService.class);
        startService(backgroundServiceIntent);
    }

    private void saveCurrentFragment(int fragIndex) {
        SharedPreferences.Editor editor = settings.edit();
        editor.putInt(getString(R.string.selected_fragment), fragIndex);
        editor.apply();
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
    }


}
