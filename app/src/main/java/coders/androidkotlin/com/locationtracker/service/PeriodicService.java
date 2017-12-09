
package coders.androidkotlin.com.locationtracker.service;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Handler;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.HashMap;
import java.util.Map;

import coders.androidkotlin.com.locationtracker.utils.GPSTracker;


public class PeriodicService extends Service {
    private static final String TAG = PeriodicService.class.getSimpleName();
    Context context;

    private Handler mPeriodicEventHandler;
    private GPSTracker gps;
    private String lat = "", lang = "";
    private int PERIODIC_EVENT_TIMEOUT = 30000;
    private LocalBroadcastManager broadcaster;

    static final public String COPA_RESULT = "com.controlj.copame.backend.COPAService.REQUEST_PROCESSED";

    static final public String COPA_MESSAGE = "com.controlj.copame.backend.COPAService.COPA_MSG";


    private Runnable doPeriodicTask = new Runnable() {
        public void run() {


            gps = new GPSTracker(PeriodicService.this);


            // check if GPS enabled

            if (gps.canGetLocation()) {

                double latitude = gps.getLatitude();
                double longitude = gps.getLongitude();

                lat = String.valueOf(latitude);
                lang = String.valueOf(longitude);
            } else {

                lat = "0.0";
                lang = "0.0";
            }


            Intent intent = new Intent(COPA_RESULT);
            intent.putExtra("lang", lang);
            intent.putExtra("lat", lat);

            broadcaster.sendBroadcast(intent);


            startService(new Intent(getApplicationContext(), PeriodicService.class));

            mPeriodicEventHandler.postDelayed(doPeriodicTask, PERIODIC_EVENT_TIMEOUT );
        }
    };

    public PeriodicService() {

    }

    @Override    public void onStart(Intent intent, int startId) {
        super.onStart(intent, startId);

        Log.e(TAG, "onStart===>" );

    }

    @Override    public void onCreate() {
        super.onCreate();

        Log.e(TAG, "onCreate ==>");
        broadcaster = LocalBroadcastManager.getInstance(PeriodicService.this);

        mPeriodicEventHandler = new Handler();
        mPeriodicEventHandler.postDelayed(doPeriodicTask, PERIODIC_EVENT_TIMEOUT);
    }

    @Override    public void onDestroy() {

        mPeriodicEventHandler.removeCallbacks(doPeriodicTask);
        super.onDestroy();
    }

    @Nullable    @Override    public IBinder onBind(Intent intent) {
        return null;
    }


}