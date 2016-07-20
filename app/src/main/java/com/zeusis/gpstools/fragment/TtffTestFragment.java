package com.zeusis.gpstools.fragment;

import android.app.Fragment;
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
import android.os.Message;
import android.os.SystemClock;
import android.preference.PreferenceManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.TextView;
import android.widget.Toast;

import com.zeusis.gpstools.R;
import com.zeusis.gpstools.service.TrackingService;
import com.zeusis.gpstools.service.TtffService;
import com.zeusis.gpstools.utils.FileUtils;
import com.zeusis.gpstools.utils.GpsUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Created by fuhuan on 2016/4/27.
 */
public class TtffTestFragment extends Fragment {

    private static final int UPDATE_VIEW = 5;
    private static final int TEST_END = 6;
    private TextView mRepeat;
    private TextView mMaxTime;
    private TextView mMode;
    private TextView mInterval;
    private TextView mGlonass;
    private TextView mStart;
    private static Button mStartButton;
    private static Button mStopButton;
    private GridView mGridView;
    private static final String TAG = "TtffTest";

    private static final int GPS_COLD_START_FLAG = 0x0;
    private static final int GPS_WARM_START_FLAG = 0x1;
    private static final int GPS_HOT_START_FLAG = 0x2;

    private int mGpsStartMode = GPS_HOT_START_FLAG;

    private static final int GPS_TTFF_TEST_START = 0x0;
    private static final int GPS_TTFF_TEST_STOP = 0x1;
    private static final int GPS_TTFF_TEST_UPDATE = 0x2;
    private static final int GPS_COMMAND_TEST_FLAG = 0x4;

    private static final int GPS_LOCATION = 0;

    private int mTtffTestRepeat;
    private int mTtffTestInterval;
    private LocationManager mLocationManager;
    private int mTtffTestCount = 0;
    private int mTtffTestMaxTime;
    private int mTimeoutCount = 0;
    private Object mLock = new Object();

    private List<Integer> mTtffList;
    private List<Long> mFixUtcList;
    private List<Double> mErrorList;
    private static HashMap<Integer, String[]> mMap;

    private TextView mKey;
    private TextView mValue;
    private static MyAdapter mAdapter;
    private volatile boolean mIsFinished = false;

    public static Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case UPDATE_VIEW:
                    mMap = (HashMap<Integer, String[]>) msg.obj;
                    if (mAdapter != null) {
                        mAdapter.notifyDataSetChanged();
                    }
                    break;
                case TEST_END:
                    mStartButton.setEnabled(true);
                    mStopButton.setEnabled(false);
                    break;

            }

        }
    };
    private FileOutputStream mTtffFos;
    private FileOutputStream mNmeaFos;
    private String mLatitude;
    private String mLongitude;
    private static Intent mIntent;
    private Button mStandaloneCold;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_ttff, null);
        mLocationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        mRepeat = (TextView) view.findViewById(R.id.tv_repeat);
        mRepeat.setText(getStringFromSetting("repeat_count"));
        mMaxTime = (TextView) view.findViewById(R.id.tv_max_time);
        mMaxTime.setText(getStringFromSetting("timeout"));
        mMode = (TextView) view.findViewById(R.id.tv_mode);
        mMode.setText(getStringFromSetting("operation_mode"));
        mInterval = (TextView) view.findViewById(R.id.tv_interval);
        mInterval.setText(getStringFromSetting("interval"));
        mGlonass = (TextView) view.findViewById(R.id.tv_glonass);
        mGlonass.setText(getStringFromSetting("glonass"));
        mStart = (TextView) view.findViewById(R.id.tv_start);
        mStart.setText(getStringFromSetting("start_mode"));

        mLatitude = getStringFromSetting("latitude");
        mLongitude = getStringFromSetting("longitude");

        view.findViewById(R.id.btn_delete_xtra).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle bundle = new Bundle();
                bundle.putBoolean("all",true);
                String strCommand = "delete_aiding_data";
                mLocationManager.sendExtraCommand(LocationManager.GPS_PROVIDER, strCommand, bundle);
            }
        });

        mStartButton = (Button) view.findViewById(R.id.btn_start);
        mStartButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!GpsUtils.checkGps(getActivity())) {
                    return;
                }
                mStartButton.setEnabled(false);
                mStandaloneCold.setEnabled(false);
                mStopButton.setEnabled(true);
                mIntent = new Intent(getActivity(), TtffService.class);
                mIntent.putExtra("standalone_cold", false);
                getActivity().startService(mIntent);

            }
        });
        mStandaloneCold = (Button) view.findViewById(R.id.btn_standalone_cold);
        mStandaloneCold.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!GpsUtils.checkGps(getActivity())) {
                    return;
                }
                mStartButton.setEnabled(false);
                mStandaloneCold.setEnabled(false);
                mStopButton.setEnabled(true);
                mIntent = new Intent(getActivity(), TtffService.class);
                mIntent.putExtra("standalone_cold", true);
                getActivity().startService(mIntent);
            }
        });
        mStopButton = (Button) view.findViewById(R.id.btn_stop);
        mStopButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mStartButton.setEnabled(true);
                mStandaloneCold.setEnabled(true);
                mStopButton.setEnabled(false);
                mIntent = new Intent(getActivity(),TtffService.class);
                getActivity().stopService(mIntent);
            }
        });

        mGridView = (GridView) view.findViewById(R.id.grid_view);
        boolean trackServiceWork = FileUtils.isTrackServiceWork(getActivity());
        boolean ttffServiceWork = FileUtils.isTtffServiceWork(getActivity());
        Log.i(TAG,"trackServiceWork = " + trackServiceWork + ", ttffServiceWork = " + ttffServiceWork);
        if (ttffServiceWork){
            mStopButton.setEnabled(true);
            mStartButton.setEnabled(false);
            mStandaloneCold.setEnabled(false);
        }
        if (trackServiceWork){
            mStopButton.setEnabled(false);
            mStartButton.setEnabled(false);
            mStandaloneCold.setEnabled(false);
        }

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();

        mMap = new HashMap<>();
        mMap.put(0, new String[]{"Lat: ", ""});
        mMap.put(1, new String[]{"Accuracy: ", ""});
        mMap.put(2, new String[]{"Lon: ", ""});
        mMap.put(3, new String[]{"Alt: ", ""});
        mMap.put(4, new String[]{"Count: ", ""});
        mMap.put(5, new String[]{"Timeout: ", ""});
        mMap.put(6, new String[]{"Status: ", ""});
        mMap.put(7, new String[]{"Prev TTFF: ", ""});
        mMap.put(8, new String[]{"Time: ", ""});
        mMap.put(9, new String[]{"Avg TTFF: ", ""});
        mAdapter = new MyAdapter();
        mGridView.setAdapter(mAdapter);
    }

    private class MyAdapter extends BaseAdapter {

        @Override
        public int getCount() {
            return mMap.size();
        }

        @Override
        public Object getItem(int position) {
            return mMap.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = View.inflate(getActivity(), R.layout.item_grid_view, null);
            }
            mKey = (TextView) convertView.findViewById(R.id.tv_key);
            mValue = (TextView) convertView.findViewById(R.id.tv_value);
            String[] items = mMap.get(position);
            mKey.setText(items[0]);
            mValue.setText(items[1]);
            return convertView;
        }
    }

    private String getStringFromSetting(String key) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());
        return prefs.getString(key, "");
    }
}