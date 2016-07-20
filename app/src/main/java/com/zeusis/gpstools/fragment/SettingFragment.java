package com.zeusis.gpstools.fragment;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.zeusis.gpstools.R;

/**
 * Created by fuhuan on 2016/4/26.
 */
public class SettingFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

    private Preference mRepeatCount;
    private Preference mStartMode;
    private Preference mGpsPlus;
    private Preference mLogFileName;
    private Preference mInterval;
    private Preference mLatitude;
    private Preference mLongitude;
    private Preference mGpsLog;
    private Preference mOperationMode;
    private Preference mTimeout;
    private Preference mSuplServer;
    private Preference mSuplServerPort;

    public SettingFragment() {
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        addPreferencesFromResource(R.xml.setting);

        mLogFileName = findPreference("log_file_name");
        mLogFileName.setSummary(mLogFileName.getSharedPreferences().getString("log_file_name", "xxxx_nmea.txt"));
        mLogFileName.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        mRepeatCount = findPreference("repeat_count");
        mRepeatCount.setSummary(mRepeatCount.getSharedPreferences().getString("repeat_count", "20"));
        mRepeatCount.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        mInterval = findPreference("interval");
        mInterval.setSummary(mInterval.getSharedPreferences().getString("interval", "50"));
        mInterval.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        mLatitude = findPreference("latitude");
        mLatitude.setSummary(mLatitude.getSharedPreferences().getString("latitude", "0.0"));
        mLatitude.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        mLongitude = findPreference("longitude");
        mLongitude.setSummary(mLongitude.getSharedPreferences().getString("longitude", "0.0"));
        mLongitude.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        mStartMode = findPreference("start_mode");
        mStartMode.setSummary(mStartMode.getSharedPreferences().getString("start_mode", "Hot Start"));
        mStartMode.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        mGpsPlus = findPreference("gps_plus");
        boolean isGpsPlus = mGpsPlus.getSharedPreferences().getBoolean("gps_plus",false);
        mGpsPlus.setSummary(isGpsPlus ? "ON" : "OFF");
        mGpsPlus.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        mGpsLog = findPreference("gps_log");
        boolean isGpsLog = mGpsLog.getSharedPreferences().getBoolean("gps_plus",false);
        mGpsLog.setSummary(isGpsLog ? "ON" : "OFF");
        mGpsLog.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        mOperationMode = findPreference("operation_mode");
        mOperationMode.setSummary(mOperationMode.getSharedPreferences().getString("operation_mode", ""));
        mOperationMode.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        mStartMode = findPreference("start_mode");
        mStartMode.setSummary(mStartMode.getSharedPreferences().getString("start_mode", ""));
        mStartMode.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        mTimeout = findPreference("timeout");
        mTimeout.setSummary(mTimeout.getSharedPreferences().getString("timeout", "180"));
        mTimeout.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        mSuplServer = findPreference("supl_server");
        mSuplServer.setSummary(mSuplServer.getSharedPreferences().getString("supl_server",""));
        mSuplServer.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        mSuplServerPort = findPreference("supl_server_port");
        mSuplServerPort.setSummary(mSuplServerPort.getSharedPreferences().getString("supl_server_port",""));
        mSuplServerPort.getSharedPreferences().registerOnSharedPreferenceChangeListener(this);

        return super.onCreateView(inflater, container, savedInstanceState);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if ("log_file_name".equals(key)){
            mLogFileName.setSummary(sharedPreferences.getString(key,""));
        }else if ("repeat_count".equals(key)) {
            mRepeatCount.setSummary(sharedPreferences.getString(key, ""));
        } else if ("interval".equals(key)){
            mInterval.setSummary(sharedPreferences.getString(key,""));
        }else if ("latitude".equals(key)){
            mLatitude.setSummary(sharedPreferences.getString(key,"0.0"));
        }else if ("longitude".equals(key)){
            mLongitude.setSummary(sharedPreferences.getString(key,"0.0"));
        }else if ("gps_plus".equals(key)){
            mGpsPlus.setSummary(sharedPreferences.getBoolean(key,false)?"ON":"OFF");
        }else if ("gps_log".equals(key)){
            mGpsLog.setSummary(sharedPreferences.getBoolean(key,false)?"ON":"OFF");
        }else if ("operation_mode".equals(key)) {
            mOperationMode.setSummary(sharedPreferences.getString(key, ""));
        }else if ("start_mode".equals(key)) {
            mStartMode.setSummary(sharedPreferences.getString(key, ""));
        }else if ("timeout".equals(key)){
            mTimeout.setSummary(sharedPreferences.getString(key,"180"));
        }else if ("supl_server".equals(key)){
            mSuplServer.setSummary(sharedPreferences.getString(key,""));
        }else if ("supl_server_port".equals(key)){
            mSuplServerPort.setSummary(sharedPreferences.getString(key,""));
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        mLogFileName.getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        mRepeatCount.getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        mInterval.getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        mLatitude.getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        mLongitude.getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        mGpsPlus.getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        mGpsLog.getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        mOperationMode.getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        mStartMode.getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        mTimeout.getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        mSuplServer.getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
        mSuplServerPort.getSharedPreferences().unregisterOnSharedPreferenceChangeListener(this);
    }
}
