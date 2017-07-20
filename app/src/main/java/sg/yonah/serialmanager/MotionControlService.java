package sg.yonah.serialmanager;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.os.Vibrator;
import android.util.Log;

/**
 * Created by litangqing on 11/7/17.
 */

public class MotionControlService extends Service implements SensorEventListener {

    private static final String TAG = "MotionControlService";

//    private float mGZ = 0;//gravity acceleration along the z axis
//    private float mGX = 0;
//    private float mGY = 0;
    private int mEventCountSinceChanged = 0;
    private static final int MAX_COUNT_GZ_CHANGE = 30;
    private boolean mStarted;
    private static final float GTOLERANCE = 5;

    private SensorManager mSensorManager;

    Vibrator vibrator ;

    long[] pattern = {0, 100, 1000};

    @Override
    public void onCreate(){
        super.onCreate();
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        Log.d(TAG, "onStartCommand, Started: " + mStarted);

        if (!mStarted) {
            mSensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
            mSensorManager.registerListener(this,
                    mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                    SensorManager.SENSOR_DELAY_GAME);

            mStarted = true;
        }
        return START_STICKY;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        int type = event.sensor.getType();
        if (type == Sensor.TYPE_ACCELEROMETER) {
            float gz = event.values[2];
            float gx = event.values[0];
            float gy = event.values[1];
            //Log.d(TAG, "gx,gy,gz: "+ gx + ", " +gy+ ", " + gz );

            if (gz<0 || gx>GTOLERANCE || gx<-GTOLERANCE || gy<-GTOLERANCE || gy >GTOLERANCE ) {
                Log.d(TAG, "now screen is facing down.");
                mEventCountSinceChanged++;
            } else{
                Log.d(TAG, "now screen is facing up");
                mEventCountSinceChanged = 0;
            }
            if (mEventCountSinceChanged>MAX_COUNT_GZ_CHANGE){
                //buzz
                vibrator.vibrate(100);
            } else{
                //
            }
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        // TODO Auto-generated method stub
    }
}


