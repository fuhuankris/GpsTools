package com.zeusis.gpstools.service;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import com.zeusis.gpstools.fragment.TrackingTestFragment;
import com.zeusis.gpstools.utils.FileUtils;
import com.zeusis.gpstools.utils.GpsUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class TrackingService extends Service {

    private static final String TAG = "TrackingTest";
    private LocationManager locationManager;
    private FileOutputStream mFos;
    private static final int GPS_NMEA_GPGGA = 2;
    private static final int GPS_NMEA_GPGSA = 3;
    private static final int GPS_NMEA_GPGSV = 4;
    private static final int GPS_STATUS = 1;
    private static final int GPS_LOCATION = 0;
    private static final int GPS_COLD_START_FLAG = 0x0;
    private static final int GPS_WARM_START_FLAG = 0x1;
    private static final int GPS_HOT_START_FLAG = 0x2;

    private int mGpsStartMode = GPS_HOT_START_FLAG;
    private FileOutputStream mTrackFileFos;
    private String mLatitude;
    private String mLongitude;
    private List<Double> mErrorList;
    private List<Double> mLatitudeList;
    private List<Double> mLongitudeList;

    public TrackingService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        Log.d(TAG, "onBind");
        throw new UnsupportedOperationException("Not yet implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
    }

    private Handler mHandler = new Handler();

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "onStartCommand");
        File file = FileUtils.getTrackFile(getApplicationContext(), "track_nmea");
        try {
            mFos = new FileOutputStream(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            Log.d(TAG, e.getMessage());
        }
        File trackFile = FileUtils.getTrackFile(getApplicationContext(), "track_", ".csv");
        try {
            mTrackFileFos = new FileOutputStream(trackFile);
            writeStr2File(mTrackFileFos, "No.,FIX UTC,ERROR\n");
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } finally {

        }
        mCount = 0;
        mLatitude = getStringFromSetting("latitude");
        mLongitude = getStringFromSetting("longitude");
        mErrorList = new ArrayList<>();
        mLatitudeList = new ArrayList<>();
        mLongitudeList = new ArrayList<>();
        getTtffTestStartMode();
        Bundle bundle = getSendCommandBundle(mGpsStartMode);
        String strCommand = "delete_aiding_data";
        locationManager.sendExtraCommand(LocationManager.GPS_PROVIDER, strCommand, bundle);
        mHandler.postDelayed(mRunable, 0);

        return super.onStartCommand(intent, flags, startId);
    }

    private Runnable mRunable = new Runnable() {
        @Override
        public void run() {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            locationManager.addGpsStatusListener(mGpsListener);
            locationManager.addNmeaListener(mNmeaListener);
        }
    };

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();
        Collections.sort(mErrorList);
        if (mErrorList.size() > 0) {
            writeStr2File(mTrackFileFos, "\n");
            writeStr2File(mTrackFileFos, " ,MIN,MAX,AVG,CEP50,CEP68,CEP95\n");
            writeStr2File(mTrackFileFos, "ERROR(m),"
                    + String.format("%8.1f", mErrorList.get(0)) + ","
                    + String.format("%8.1f", mErrorList.get(mErrorList.size() - 1)) + ","
                    + String.format("%8.1f", getAvgTtff(mErrorList)) + ","
                    + String.format("%8.1f", GpsUtils.CEP(mErrorList,50)) + ","
                    + String.format("%8.1f", GpsUtils.CEP(mErrorList, 68)) + ","
                    + String.format("%8.1f", GpsUtils.CEP(mErrorList, 95)) + "\n");
        }

        try {
            if (mFos != null) {
                mFos.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (mTrackFileFos != null) {
                mTrackFileFos.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (mRunable != null) {
            mHandler.removeCallbacks(mRunable);
        }
        locationManager.removeUpdates(locationListener);
        locationManager.removeGpsStatusListener(mGpsListener);
        locationManager.removeNmeaListener(mNmeaListener);
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

    GpsStatus.NmeaListener mNmeaListener = new GpsStatus.NmeaListener() {

        @Override
        public void onNmeaReceived(long timestamp, String nmea) {

            try {
                mFos.write(nmea.getBytes());
                mFos.flush();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (nmea.startsWith("$GPGGA")) {
                Message gpggaMessage = TrackingTestFragment.mHandler.obtainMessage();
                gpggaMessage.obj = nmea;
                gpggaMessage.what = GPS_NMEA_GPGGA;
                TrackingTestFragment.mHandler.sendMessage(gpggaMessage);
            } else if (nmea.startsWith("$GPGSA")) {
                Message gpgsaMessage = TrackingTestFragment.mHandler.obtainMessage();
                gpgsaMessage.obj = nmea;
                gpgsaMessage.what = GPS_NMEA_GPGSA;
                TrackingTestFragment.mHandler.sendMessage(gpgsaMessage);
            } else if (nmea.startsWith("$GPGSV")) {
                Message gpgsvMessage = TrackingTestFragment.mHandler.obtainMessage();
                gpgsvMessage.obj = nmea;
                gpgsvMessage.what = GPS_NMEA_GPGSV;
                TrackingTestFragment.mHandler.sendMessage(gpgsvMessage);
            }
        }
    };

    GpsStatus.Listener mGpsListener = new GpsStatus.Listener() {
        public void onGpsStatusChanged(int event) {
            switch (event) {
                case GpsStatus.GPS_EVENT_FIRST_FIX:
                    //Toast.makeText(getApplicationContext(), "mGpsStatusListener =  ", Toast.LENGTH_SHORT).show();
                    Log.d(TAG, "GPS_EVENT_FIRST_FIX");
                    break;

                case GpsStatus.GPS_EVENT_SATELLITE_STATUS:
                    Log.d(TAG, "GPS_EVENT_SATELLITE_STATUS");

                    GpsStatus gpsStatus = locationManager.getGpsStatus(null);
                    int maxSatellites = gpsStatus.getMaxSatellites();
                    //System.out.println("maxSatellites = " + maxSatellites);
                    Iterator<GpsSatellite> iters = gpsStatus.getSatellites().iterator();
                    int count = 0;
                    int usedCount = 0;
                    while (iters.hasNext() && count <= maxSatellites) {
                        GpsSatellite gpsSatellite = iters.next();
                        /*if (gpsSatellite.usedInFix()) {
                            usedCount++;
                        }*/
                        if (gpsSatellite.getSnr() > 0) {
                            usedCount++;
                        }
                        gpsSatellite.getPrn();
                        count++;
                    }
                    Log.d(TAG, "GPS--count = " + count);
                    Log.d(TAG, "GPS--usedCount = " + usedCount);
                    System.out.println("getTimeToFirstFix = " + gpsStatus.getTimeToFirstFix());
                    Message message = Message.obtain();
                    message.what = GPS_STATUS;
                    message.arg1 = count;
                    message.arg2 = usedCount;
                    message.obj = gpsStatus;
                    //Toast.makeText(getApplicationContext(), "count =" + count + ", usedCount = " + usedCount, Toast.LENGTH_SHORT).show();
                    TrackingTestFragment.mHandler.sendMessage(message);
                    break;

                case GpsStatus.GPS_EVENT_STARTED:
                    Log.d(TAG, "GPS_EVENT_STARTED");
                    break;

                case GpsStatus.GPS_EVENT_STOPPED:
                    Log.d(TAG, "GPS_EVENT_STOPPED");
                    break;
            }
        }
    };

    private int mCount = 0;

    LocationListener locationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            mCount++;
            //Toast.makeText(getApplicationContext(), "mLocationListener = ", Toast.LENGTH_SHORT).show();
            Message locationMessage = TrackingTestFragment.mHandler.obtainMessage();
            locationMessage.obj = location;
            locationMessage.what = GPS_LOCATION;
            TrackingTestFragment.mHandler.sendMessage(locationMessage);

            mLatitudeList.add(GpsUtils.getDistance(location.getLatitude(), 0, Double.valueOf(mLatitude), 0));
            mLongitudeList.add(GpsUtils.getDistance(0, location.getLongitude(), 0, Double.valueOf(mLongitude)));
            double distance = GpsUtils.getDistance(location.getLatitude(), location.getLongitude(),
                    Double.valueOf(mLatitude), Double.valueOf(mLongitude));
            mErrorList.add(distance);
            writeStr2File(mTrackFileFos, mCount + "," +
                    FileUtils.formatTimeForFile(location.getTime()) + "," +
                    String.format("%8.1f", distance) + "\n");
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        public void onProviderEnabled(String provider) {
        }

        public void onProviderDisabled(String provider) {
        }
    };

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

    private String getStringFromSetting(String key) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getApplicationContext());
        return prefs.getString(key, "");
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
}
