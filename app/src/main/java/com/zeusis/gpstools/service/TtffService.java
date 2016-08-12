package com.zeusis.gpstools.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.zeusis.gpstools.fragment.TtffTestFragment;
import com.zeusis.gpstools.utils.FileUtils;
import com.zeusis.gpstools.utils.GpsUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public class TtffService extends Service {

    private FileOutputStream mTtffFos;
    private FileOutputStream mNmeaFos;
    //private MyCountDownTimer mMyCountDownTimer;
    private MyRuningTime mRuningTime;
    private MyWaitingTime mWaitingTime;
    private String mLatitude;
    private String mLongitude;
    private static final String TAG = "TtffTest";

    private static final int GPS_COLD_START_FLAG = 0x0;
    private static final int GPS_WARM_START_FLAG = 0x1;
    private static final int GPS_HOT_START_FLAG = 0x2;

    private int mGpsStartMode = GPS_HOT_START_FLAG;

    private static final int GPS_TTFF_TEST_START = 0x0;
    private static final int GPS_TTFF_TEST_STOP = 0x1;
    private static final int GPS_TTFF_TEST_UPDATE = 0x2;
    private static final int GPS_COMMAND_TEST_FLAG = 0x4;
    private static final int UPDATE_VIEW = 5;
    private static final int TEST_END = 6;
    public static final String ASSISTED_GPS_ENABLED = "assisted_gps_enabled";

    private static final int GPS_LOCATION = 0;

    private int mTtffTestRepeat;
    private int mTtffTestInterval;
    private LocationManager mLocationManager;
    private int mTtffTestCount = 0;
    private int mTtffTestMaxTime;
    private int mTimeoutCount = 0;
    private Object mLock = new Object();
    private List<Double> mTtffList;
    private List<Long> mFixUtcList;
    private List<Double> mErrorList;
    private HashMap<Integer, String[]> mMap;
    private volatile boolean mIsFinished = false;
    private boolean mStandaloneCold = false;

    private Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {

            switch (msg.what) {
                case GPS_TTFF_TEST_START:
                    Log.d(TAG, "GPS_TTFF_TEST_START");
                    mMap.put(0, new String[]{"Lat: ", "0.0"});
                    mMap.put(1, new String[]{"Accuracy: ", "0.0"});
                    mMap.put(2, new String[]{"Lon: ", "0.0"});
                    mMap.put(3, new String[]{"Alt: ", "0.0"});
                    mMap.put(4, new String[]{"Count: ", "" + mTtffTestCount + "/" + mTtffTestRepeat});
                    mMap.put(5, new String[]{"Timeout: ", "0"});
                    mMap.put(6, new String[]{"Status: ", "running"});
                    mMap.put(7, new String[]{"Prev TTFF: ", "0"});
                    mMap.put(8, new String[]{"Time: ", "00:00"});
                    mMap.put(9, new String[]{"Avg TTFF: ", "0"});
                    Message viewMessage = mFragmentHandler.obtainMessage();
                    viewMessage.what = UPDATE_VIEW;
                    viewMessage.obj = mMap;
                    mFragmentHandler.sendMessage(viewMessage);
                    break;
                case GPS_TTFF_TEST_UPDATE:
                    Log.d(TAG, "GPS_TTFF_TEST_UPDATE");
                    stopTtffTest();
                    if (mRuningTime != null) {
                        mRuningTime.isRunning = false;
                    }
                    /*if (mMyCountDownTimer != null) {
                        mMyCountDownTimer.cancel();
                    }*/
                    removeCallbacks(mTimeoutTimer);
                    if (mLocation != null) {
                        //mMap.put(0, new String[]{"Lat: ", "" + mLocation.getLatitude()});
                        mMap.put(0, new String[]{"Lat: ", String.format("%3.8f", mLocation.getLatitude())});
                        mMap.put(1, new String[]{"Accuracy: ", "" + mLocation.getAccuracy()});
                        //mMap.put(2, new String[]{"Lon: ", "" + mLocation.getLongitude()});
                        mMap.put(2, new String[]{"Lon: ", String.format("%3.8f", mLocation.getLongitude())});
                        mMap.put(3, new String[]{"Alt: ", "" + mLocation.getAltitude()});
                    }
                    mMap.put(4, new String[]{"Count: ", "" + (mTtffTestCount + 1) + "/" + mTtffTestRepeat});
                    mMap.put(5, new String[]{"Timeout: ", "" + mTimeoutCount});
                    mMap.put(6, new String[]{"Status: ", "waiting"});
                    mMap.put(7, new String[]{"Prev TTFF: ", String.format("%3.2f", mTtffList.get(mTtffTestCount - 1) / 1000)});
                    mMap.put(8, new String[]{"Time: ", ""});
                    mMap.put(9, new String[]{"Avg TTFF: ", "" + String.format("%3.2f", getAvgTtff(mTtffList) / 1000)});
                    Message viewMessage2 = mFragmentHandler.obtainMessage();
                    viewMessage2.what = UPDATE_VIEW;
                    viewMessage2.obj = mMap;
                    mFragmentHandler.sendMessage(viewMessage2);
                    if (mTtffTestCount == mTtffTestRepeat) {
                        mHandler.sendEmptyMessage(GPS_TTFF_TEST_STOP);
                        return;
                    } else {
                        mWaitingTime = new MyWaitingTime();
                        mWaitingTime.execute();
                        postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                startTtffTest();
                            }
                        }, mTtffTestInterval * 1000);
                    }

                    break;
                case GPS_TTFF_TEST_STOP:
                    mIsFinished = true;
                    if (mRuningTime != null) {
                        mRuningTime.isRunning = false;
                    }
                    if (mWaitingTime != null) {
                        mWaitingTime.isRunning = false;
                    }
                    removeCallbacks(mTimeoutTimer);
                    Log.d(TAG, "GPS_TTFF_TEST_STOP");
                    mMap.put(0, new String[]{"Lat: ", "0.0"});
                    mMap.put(1, new String[]{"Accuracy: ", "0.0"});
                    mMap.put(2, new String[]{"Lon: ", "0.0"});
                    mMap.put(3, new String[]{"Alt: ", "0.0"});
                    //mMap.put(4, new String[]{"Count: ", ""});
                    mMap.put(4, new String[]{"Count: ", "" + (mTtffTestCount) + "/" + mTtffTestRepeat});
                    mMap.put(5, new String[]{"Timeout: ", "" + mTimeoutCount});
                    mMap.put(6, new String[]{"Status: ", "--"});
                    mMap.put(8, new String[]{"Time: ", "00:00"});
                    try {
                        if (mTtffList.size() > 0) {
                            mMap.put(7, new String[]{"Prev TTFF: ", String.format("%3.2f", mTtffList.get(mTtffTestCount - 1) / 1000)});
                            mMap.put(9, new String[]{"Avg TTFF: ", String.format("%3.2f", getAvgTtff(mTtffList) / 1000)});
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.d(TAG, e.getMessage());
                    }
                    Message viewMessage3 = mFragmentHandler.obtainMessage();
                    viewMessage3.what = UPDATE_VIEW;
                    viewMessage3.obj = mMap;
                    mFragmentHandler.sendMessage(viewMessage3);
                    mTtffTestCount = 0;
                    mTimeoutCount = 0;
                    mLocationManager.removeUpdates(mLocationListener);
                    mLocationManager.removeNmeaListener(mNmeaListener);
                    mLocationManager.removeGpsStatusListener(mGpsStatusListener);
                    writeStr2File(mTtffFos, "AVG," + String.format("%3.2f", (float) getAvgTtff(mTtffList) / 1000) + "," + mTz + "," + String.format("%8.1f", getAvgTtff(mErrorList)) + "\n");
                    writeStr2File(mTtffFos, "\n");
                    writeStr2File(mTtffFos, " ,LATITUDE,LONGITUDE\n");
                    writeStr2File(mTtffFos, "TRUTH," + mLatitude + "," + mLongitude + "\n");
                    writeStr2File(mTtffFos, "\n");
                    writeStr2File(mTtffFos, " ,MIN,MAX,AVG,CEP50,CEP68,CEP95\n");
                    Collections.sort(mTtffList);
                    Collections.sort(mErrorList);
                    //if (mTtffTestCount > 94) {
                    try {
                        if (mTtffList.size() > 0) {
                            writeStr2File(mTtffFos, "TTFF(s),"
                                    + String.format("%3.2f", mTtffList.get(0) / 1000) + ","
                                    + String.format("%3.2f", mTtffList.get(mTtffList.size() - 1) / 1000) + ","
                                    + String.format("%3.2f", getAvgTtff(mTtffList) / 1000) + ","
                                    + String.format("%3.2f", GpsUtils.CEP(mTtffList,50) / 1000) + ","
                                    + String.format("%3.2f", GpsUtils.CEP(mTtffList,68) / 1000) + ","
                                    + String.format("%3.2f", GpsUtils.CEP(mTtffList,95) / 1000) + "\n");
                            writeStr2File(mTtffFos, "ERROR(m),"
                                    + String.format("%8.1f", mErrorList.get(0)) + ","
                                    + String.format("%8.1f", mErrorList.get(mErrorList.size() - 1)) + ","
                                    + String.format("%8.1f", getAvgTtff(mErrorList)) + ","
                                    + String.format("%8.1f", GpsUtils.CEP(mErrorList,50)) + ","
                                    + String.format("%8.1f", GpsUtils.CEP(mErrorList, 68)) + ","
                                    + String.format("%8.1f", GpsUtils.CEP(mErrorList,95)) + "\n");
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.d(TAG, e.getMessage());
                    }
                    sleepTime(1000);
                    mFragmentHandler.sendEmptyMessage(TEST_END);
                    FileUtils.closeFos(mTtffFos);
                    FileUtils.closeFos(mNmeaFos);
                    stopSelf();
                    break;

            }
        }

    };
    private Handler mFragmentHandler;
    private String mTz;

    public TtffService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        throw new UnsupportedOperationException("Not yet implemented");
    }

    private List<Double> mLatitudeList;
    private List<Double> mLongitudeList;

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        mFragmentHandler = TtffTestFragment.mHandler;
        mMap = new HashMap<>();
        mLocationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        mTtffList = new ArrayList<>();
        mFixUtcList = new ArrayList<>();
        mErrorList = new ArrayList<>();
        mLatitudeList = new ArrayList<>();
        mLongitudeList = new ArrayList<>();
        mLatitude = getStringFromSetting("latitude");
        mLongitude = getStringFromSetting("longitude");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        mIsFinished = false;
        //mTtffList.clear();
        //mFixUtcList.clear();
        //mErrorList.clear();
        mTtffTestCount = 0;
        mTimeoutCount = 0;
        if (intent != null) {
            mStandaloneCold = intent.getBooleanExtra("standalone_cold",false);
        }
        getTtffTestInterval();
        getTtffTestMaxTime();
        getTtffTestRepeat();
        getTtffTestStartMode();

        TimeZone timeZone = TimeZone.getDefault();
        mTz = timeZone.getDisplayName(false, TimeZone.SHORT);

        File ttffFile = FileUtils.getTtffFile(getApplicationContext(), "ttff_", ".csv");
        try {
            mTtffFos = new FileOutputStream(ttffFile);
            writeStr2File(mTtffFos, "No.,TTFF,FIX UTC,ERROR\n");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {

        }
        File mNmeaFile = FileUtils.getTtffFile(getApplicationContext(), "ttff_nmea_", ".txt");
        try {
            mNmeaFos = new FileOutputStream(mNmeaFile);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        Bundle bundle = getSendCommandBundle(mGpsStartMode);
        String strCommand = "delete_aiding_data";
        mLocationManager.sendExtraCommand(LocationManager.GPS_PROVIDER, strCommand, bundle);
        mHandler.sendEmptyMessage(GPS_TTFF_TEST_START);
        sleepTime(1000);
        startTtffTest();
        return super.onStartCommand(intent, flags, startId);
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
        mIsFinished = true;
        if (mRuningTime != null) {
            mRuningTime.isRunning = false;
        }
        /*if (mMyCountDownTimer != null) {
            mMyCountDownTimer.cancel();
        }*/
        if (mWaitingTime != null) {
            mWaitingTime.isRunning = false;
        }
        mHandler.removeCallbacks(mTimeoutTimer);
        mHandler.removeCallbacks(mStartTestRunnable);
        mHandler.sendEmptyMessage(GPS_TTFF_TEST_STOP);
    }

    private class MyWaitingTime extends AsyncTask<Void, Integer, Void> {
        public volatile boolean isRunning = true;

        @Override
        protected Void doInBackground(Void... params) {
            for (int i = 0; i < mTtffTestInterval; i++) {
                if (!isRunning) {
                    break;
                }
                publishProgress(mTtffTestInterval - i);
                SystemClock.sleep(1000);
            }
            return null;
        }

        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (mRuningTime != null) {
                mRuningTime.isRunning = false;
            }
            mMap.put(6, new String[]{"Status: ", "waiting"});
            sendUpdateMessage(mMap);
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            mMap.put(8, new String[]{"Time: ", FileUtils.formatNum2Time(values[0])});
            sendUpdateMessage(mMap);
        }
    }

    private void sendUpdateMessage(Map<Integer, String[]> map) {
        Message message = mFragmentHandler.obtainMessage();
        message.what = UPDATE_VIEW;
        message.obj = map;
        mFragmentHandler.sendMessage(message);
    }

    private class MyRuningTime extends AsyncTask<Void, Integer, Void> {

        public volatile boolean isRunning = true;

        @Override
        protected Void doInBackground(Void... params) {
            for (int i = 0; i < mTtffTestMaxTime; i++) {
                if (!isRunning) {
                    break;
                }
                publishProgress(i);
                SystemClock.sleep(1000);
            }
            return null;
        }


        @Override
        protected void onPreExecute() {
            super.onPreExecute();
            if (mWaitingTime != null) {
                mWaitingTime.isRunning = false;
            }
            mMap.put(6, new String[]{"Status: ", "running"});
            sendUpdateMessage(mMap);
        }

        @Override
        protected void onProgressUpdate(Integer... values) {
            mMap.put(8, new String[]{"Time: ", FileUtils.formatNum2Time(values[0])});
            sendUpdateMessage(mMap);
        }
    }

    private class MyCountDownTimer extends CountDownTimer {
        /**
         * @param millisInFuture    The number of millis in the future from the call
         *                          to {@link #start()} until the countdown is done and {@link #onFinish()}
         *                          is called.
         * @param countDownInterval The interval along the way to receive
         *                          {@link #onTick(long)} callbacks.
         */
        public MyCountDownTimer(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onTick(long millisUntilFinished) {

        }

        @Override
        public void onFinish() {
            Log.d(TAG, "onFinish" + mIsFinished);
            if (mIsFinished) {
                return;
            }
            mTimeoutCount++;

            mTtffList.add(mTtffTestMaxTime * 1000d);
            mErrorList.add(0d);
            mFixUtcList.add(0L);
            mHandler.sendEmptyMessage(GPS_TTFF_TEST_UPDATE);
            //writeStr2File(mTtffFos, mTtffTestCount + "," + mTtffList.get(mTtffTestCount - 1) + "," + mFixUtcList.get(mTtffTestCount - 1) + "," + mErrorList.get(mTtffTestCount - 1) + "\n");
        }
    }

    private Runnable mTimeoutTimer = new Runnable() {
        @Override
        public void run() {
            Log.d(TAG, "mTimeoutTimer = " + mIsFinished);
            if (mIsFinished) {
                return;
            }
            mTimeoutCount++;

            mTtffList.add(mTtffTestMaxTime * 1000d);
            mErrorList.add(0d);
            mFixUtcList.add(0L);
            mHandler.sendEmptyMessage(GPS_TTFF_TEST_UPDATE);
        }
    };

    private GpsStatus.NmeaListener mNmeaListener = new GpsStatus.NmeaListener() {

        @Override
        public void onNmeaReceived(long timestamp, String nmea) {
            writeStr2File(mNmeaFos, FileUtils.formatTime(System.currentTimeMillis()) + "\t" + nmea);
        }
    };

    private Location mLocation;
    LocationListener mLocationListener = new LocationListener() {
        public void onLocationChanged(Location location) {

            mLocation = location;
            //Toast.makeText(getApplicationContext(), "mLocationListener = ", Toast.LENGTH_SHORT).show();
            mFixUtcList.add(location.getTime());
            //Log.d(TAG, "mLatitude" + Double.valueOf(mLatitude));
            //Toast.makeText(getApplicationContext(), "mLatitude" + Double.valueOf(mLatitude), Toast.LENGTH_SHORT).show();
            mLatitudeList.add(GpsUtils.getDistance(location.getLatitude(), 0, Double.valueOf(mLatitude), 0));
            mLongitudeList.add(GpsUtils.getDistance(0, location.getLongitude(), 0, Double.valueOf(mLongitude)));
            mErrorList.add(GpsUtils.getDistance(location.getLatitude(), location.getLongitude(),
                    Double.valueOf(mLatitude), Double.valueOf(mLongitude)));
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        public void onProviderEnabled(String provider) {
        }

        public void onProviderDisabled(String provider) {
        }
    };

    private GpsStatus.Listener mGpsStatusListener = new GpsStatus.Listener() {
        public void onGpsStatusChanged(int event) {
            if (event == GpsStatus.GPS_EVENT_FIRST_FIX) {
                //Toast.makeText(getApplicationContext(), "mGpsStatusListener = ", Toast.LENGTH_SHORT).show();
                synchronized (mLock) {
                    if (mTtffTestRepeat != 0) {
                        GpsStatus gpsStatus = mLocationManager.getGpsStatus(null);
                        int ttff = gpsStatus.getTimeToFirstFix();
                        mTtffList.add(Double.valueOf(ttff + ""));
                        mHandler.sendEmptyMessage(GPS_TTFF_TEST_UPDATE);
                    }
                }
            }
        }
    };

    private int getAvgTtffI(List<Integer> list) {
        if (list == null) {
            return 0;
        }
        int size = list.size();
        int sum = 0;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) == 0) {
                size--;
            }
            sum += list.get(i);
        }
        if (list.size() == 0) {
            return 0;
        }
        return (int) (sum / size);
    }

    private double getAvgTtff(List<Double> list) {
        if (list == null) {
            return 0;
        }
        int size = list.size();
        double sum = 0;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i) == 0) {
                size--;
            }
            sum += list.get(i);
        }
        if (list.size() == 0) {
            return 0;
        }
        return sum / size;
    }

    private void startTtffTest() {
        Log.d(TAG, "startTtffTest");
        if (mIsFinished) {
            return;
        }
        mTtffTestCount++;
        mRuningTime = new MyRuningTime();
        mRuningTime.execute();
        //mMyCountDownTimer = new MyCountDownTimer(mTtffTestMaxTime * 1000, 1000);
        //mMyCountDownTimer.start();
        mHandler.postDelayed(mTimeoutTimer,mTtffTestMaxTime * 1000);
        if (mTtffTestCount > mTtffTestRepeat) {
            mHandler.sendEmptyMessage(GPS_TTFF_TEST_STOP);
            return;
        }
        Bundle bundle;
        if (mStandaloneCold){
            bundle = new Bundle();
            bundle.putBoolean("all", true);
        }else {
            bundle = getSendCommandBundle(mGpsStartMode);
        }
        String strCommand = "delete_aiding_data";
        mLocationManager.sendExtraCommand(LocationManager.GPS_PROVIDER, strCommand, bundle);
        mHandler.postDelayed(mStartTestRunnable, 0);

    }

    private Runnable mStartTestRunnable = new Runnable() {
        @Override
        public void run() {
            mLocationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    mTtffTestMaxTime * 1000, 10000000, mLocationListener);
            mLocationManager.addNmeaListener(mNmeaListener);
            mLocationManager.addGpsStatusListener(mGpsStatusListener);
        }
    };

    private void stopTtffTest() {
        Log.d(TAG, "stopTtffTest");
        //mIsFinished = true;
        try {
            //Toast.makeText(getApplicationContext(), "" + mTtffList, Toast.LENGTH_SHORT).show();
            Log.d(TAG, "(mTtffList.get(mTtffTestCount - 1) / 1000) = " + (mTtffList.get(mTtffTestCount - 1) / 1000));
            //String.format("%3.2f", (float) mTtffList.get(mTtffTestCount - 1) / 1000);
            writeStr2File(mTtffFos, mTtffTestCount + "," + String.format("%3.2f", mTtffList.get(mTtffTestCount - 1) / 1000) + "," + FileUtils.formatTimeForFile(mFixUtcList.get(mTtffTestCount - 1)) + "," + String.format("%8.1f", mErrorList.get(mTtffTestCount - 1)) + "\n");
        } catch (Exception e) {
            e.printStackTrace();
            Log.d(TAG, e.getMessage());
        }
        //int anInt = Settings.Global.getInt(getContentResolver(), ASSISTED_GPS_ENABLED, 0);
        //Log.d(TAG,"anInt = " + anInt);
        mLocationManager.removeUpdates(mLocationListener);
        mLocationManager.removeGpsStatusListener(mGpsStatusListener);
    }

    private void writeStr2File(FileOutputStream fos, String str) {
        if (fos == null) {
            return;
        }
        try {
            fos.write(str.getBytes());
            fos.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private String getStringFromSetting(String key) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        return prefs.getString(key, "");
    }

    private void getTtffTestStartMode() {
        String str = getStringFromSetting("start_mode").trim();
        //str = mStart.getText().toString().trim();
        Log.d(TAG, "getTtffTestMode: " + str);

        if (str.equals("Cold Start")) {
            mGpsStartMode = GPS_COLD_START_FLAG;
        } else if (str.equals("Warm Start")) {
            mGpsStartMode = GPS_WARM_START_FLAG;
        } else if (str.equals("Hot Start")) {
            mGpsStartMode = GPS_HOT_START_FLAG;
        } else {
            mGpsStartMode = GPS_HOT_START_FLAG;
        }
    }

    private Bundle getSendCommandBundle(int flag) {
        Bundle bundle = new Bundle();

        if (flag == GPS_COLD_START_FLAG) {
            //bundle.putBoolean("all", true);
            bundle.putBoolean("ephemeris", true);
            bundle.putBoolean("almanac", true);
            bundle.putBoolean("position", true);
            bundle.putBoolean("time", true);
            bundle.putBoolean("iono", true);
            bundle.putBoolean("utc", true);
            bundle.putBoolean("health", true);
            bundle.putBoolean("svdir", true);
            bundle.putBoolean("svsteer", true);
            bundle.putBoolean("sadata", true);
            bundle.putBoolean("rti", true);
            bundle.putBoolean("celldb-info", true);
        } else if (flag == GPS_WARM_START_FLAG) {
            bundle.putBoolean("ephemeris", true);
        }
        return bundle;
    }

    private void getTtffTestRepeat() {
        String sum = getStringFromSetting("repeat_count").trim();
        Log.d(TAG, "getTtffTestSum: " + sum);
        mTtffTestRepeat = Integer.parseInt(sum);
        if (mTtffTestRepeat > 10000) {
            mTtffTestRepeat = 10000;
            Log.d(TAG, "getTtffTestRepeat: " + mTtffTestRepeat);
        }
    }

    private void getTtffTestInterval() {
        String sum = getStringFromSetting("interval").trim();
        Log.d(TAG, "getTtffTestSum: " + sum);
        mTtffTestInterval = Integer.parseInt(sum);
        if (mTtffTestInterval < 0) {
            mTtffTestInterval = 0;
            Log.d(TAG, "getTtffTestInterval: " + mTtffTestInterval);
        }
    }

    private void getTtffTestMaxTime() {
        String sum = getStringFromSetting("timeout").trim();
        Log.d(TAG, "getTtffTestMaxTime: " + sum);
        mTtffTestMaxTime = Integer.parseInt(sum);
    }

    private void sleepTime(long timeMillions) {
        SystemClock.sleep(timeMillions);
    }
}
