package com.googlemap.mapmusicapp;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorManager;
import android.os.IBinder;
import android.os.PowerManager;

public class StepCounterService extends Service {

    public static boolean FLAG = false;
    private SensorManager mSensorManager;
    private StepDetector mStepDetector;
    private PowerManager mPowerManager;
    private PowerManager.WakeLock mWakeLock;

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @SuppressLint("InvalidWakeLockTag")
    @SuppressWarnings("deprecation")
    @Override
    public void onCreate() {
        super.onCreate();
        FLAG = true;
        mStepDetector = new StepDetector(this);
        mSensorManager = (SensorManager) this.getSystemService(SENSOR_SERVICE);
        mSensorManager.registerListener(mStepDetector,
                mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER),
                SensorManager.SENSOR_DELAY_FASTEST);

        mPowerManager = (PowerManager) this.getSystemService(Context.POWER_SERVICE);
        mWakeLock = mPowerManager.newWakeLock(PowerManager.SCREEN_DIM_WAKE_LOCK|PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "s");
        mWakeLock.acquire();
    }

    @Override
    public void onDestroy() {
        // TODO Auto-generated method stub
        super.onDestroy();
        FLAG = false;
        if(mStepDetector != null){
            mSensorManager.unregisterListener(mStepDetector);
        }
        if(mWakeLock != null){
            mWakeLock.release();
        }
    }
}
