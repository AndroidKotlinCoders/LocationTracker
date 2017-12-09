package coders.androidkotlin.com.locationtracker;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import java.util.ArrayList;

import coders.androidkotlin.com.locationtracker.service.PeriodicService;
import coders.androidkotlin.com.locationtracker.utils.GPSTracker;
import coders.androidkotlin.com.locationtracker.utils.PermissionUtils;

import static coders.androidkotlin.com.locationtracker.service.PeriodicService.COPA_RESULT;

public class MainActivity extends AppCompatActivity implements ActivityCompat.OnRequestPermissionsResultCallback,
        PermissionUtils.PermissionResultCallback {

    private static final String TAG = "MainActivity";
    Intent intentservice;
    BroadcastReceiver receiver;
    TextView txt_lat, txt_lang;
    PermissionUtils permissionUtils;
    ArrayList<String> permissions = new ArrayList<>();
    boolean isPermissionGranted;
    Handler handlerForGPS;

    Runnable runnable = new Runnable() {
        @Override
        public void run() {

            LocationManager lm = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);

            if (ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.this, android.Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                // TODO: Consider calling
                return;
            }

            GPSTracker gps = new GPSTracker(MainActivity.this);
            if (gps.canGetLocation()) {

                double latitude = gps.getLatitude();
                double longitude = gps.getLongitude();

                if (latitude > 0.0 && longitude > 0.0) {
                    handlerForGPS.removeCallbacks(runnable);
                    startServcieForLocation();
                } else {
                    handlerForGPS.postDelayed(runnable, 2000);
                }

            } else {

                Log.e(TAG, "Location is not available and Recheck GPS. ==>");
                GPSEnabled();

            }


        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        txt_lat = findViewById(R.id.lat);
        txt_lang = findViewById(R.id.lang);

        Button btn_start = findViewById(R.id.btn_start);
        Button btn_stop = findViewById(R.id.btn_stop);

        permissionUtils = new PermissionUtils(MainActivity.this);
        permissions.add(android.Manifest.permission.ACCESS_FINE_LOCATION);
        permissions.add(android.Manifest.permission.ACCESS_COARSE_LOCATION);
        permissionUtils.check_permission(permissions, "Need GPS permission for getting your location", 1);


        receiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                String lang = intent.getStringExtra("lang");
                String lat = intent.getStringExtra("lat");
                if (lat.equalsIgnoreCase("0.0") && lang.equalsIgnoreCase("0.0")) {

                    GPSEnabled();
                }

                txt_lat.setText("Latitude : " + lat);
                txt_lang.setText("Logitude : " + lang);

            }
        };

        btn_start.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                startServcieForLocation();
            }
        });


        btn_stop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                txt_lat.setText(" ");
                txt_lang.setText(" ");
                stopService(intentservice);
            }
        });


    }

    private void GPSEnabled() {

        boolean gps_enabled = false;
        boolean network_enabled = false;
        AlertDialog.Builder dialog = null;

        LocationManager lm = (LocationManager) getApplicationContext().getSystemService(Context.LOCATION_SERVICE);
        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ex) {
        }

        if (!gps_enabled && !network_enabled) {
            // notify user
            dialog = new AlertDialog.Builder(MainActivity.this);
            dialog.setMessage(MainActivity.this.getResources().getString(R.string.gps_network_not_enabled));
            dialog.setCancelable(false);
            dialog.setPositiveButton(MainActivity.this.getResources().getString(R.string.open_location_settings), new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {

                    Log.i(TAG, "ACTION_LOCATION_SOURCE_SETTINGS===>");

                    // TODO Auto-generated method stub
                    Intent myIntent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                    startActivityForResult(myIntent, 100);
                    //get gps
                }
            });
            dialog.setNegativeButton(MainActivity.this.getString(R.string.Cancel), new DialogInterface.OnClickListener() {

                @Override
                public void onClick(DialogInterface paramDialogInterface, int paramInt) {


                    GPSEnabled();

                    // TODO Auto-generated method stub

                }
            });
            dialog.show();
        } else {

            GPSTracker gps = new GPSTracker(getApplicationContext());

            if (dialog != null) {
                dialog.show().dismiss();
            }

            if (gps.canGetLocation()) {

                double latitude = gps.getLatitude();
                double longitude = gps.getLongitude();

                if (latitude > 0.0 && longitude > 0.0) {

                    startServcieForLocation();

                } else {
                    handlerForGPS = new Handler();
                    handlerForGPS.postDelayed(runnable, 1000);
                }
            }

        }


    }

    public void startServcieForLocation() {

        intentservice = new Intent(MainActivity.this, PeriodicService.class);
        startService(intentservice);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        Log.e(TAG, "onActivityResult");
        switch (requestCode) {

            case 100:

                Log.e(TAG, "call back");

                GPSEnabled();

                break;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {

        permissionUtils.onRequestPermissionsResult(requestCode, permissions, grantResults);


    }

    @Override
    public void PermissionGranted(int request_code) {
        Log.e(TAG, "GRANTED");
        isPermissionGranted = true;
        GPSEnabled();
    }

    @Override
    public void PartialPermissionGranted(int request_code, ArrayList<String> granted_permissions) {
        Log.e(TAG, "Partial GRANTED");
    }

    @Override
    public void PermissionDenied(int request_code) {
        Log.i("PERMISSION", "DENIED");
    }

    @Override
    public void NeverAskAgain(int request_code) {
        Log.i("PERMISSION", "NEVER ASK AGAIN");
    }

    @Override
    protected void onStart() {
        super.onStart();

        LocalBroadcastManager.getInstance(this).registerReceiver((receiver), new IntentFilter(COPA_RESULT)
        );
    }

    @Override
    protected void onStop() {
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        super.onStop();
    }
}

