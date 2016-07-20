package com.zeusis.gpstools.fragment;

import android.app.Fragment;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.location.GpsSatellite;
import android.location.GpsStatus;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.SystemClock;
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
import com.zeusis.gpstools.utils.FileUtils;
import com.zeusis.gpstools.utils.GpsUtils;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * Created by fuhuan on 2016/4/22.
 */
public class TrackingTestFragment extends Fragment {

    private static final String TAG = "TrackingTest";
    private static final int GPS_NMEA_GPGGA = 2;
    private static final int GPS_NMEA_GPGSA = 3;
    private static final int GPS_NMEA_GPGSV = 4;
    private TextView mKey;
    private TextView mValue;
    private static final int GPS_STATUS = 1;
    private static final int GPS_LOCATION = 0;
    private GridView mGridView;
    private static Map<Integer, String[]> mMap;
    private static MyAdapter mAdapter;
    private static TextView mUtcTime;
    private FileOutputStream mFos;
    private Button mStart;
    private Button mStop;
    private static Intent intent;
    private LocationManager mLocationManager;

    public TrackingTestFragment() {
    }

    public static Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case GPS_LOCATION:
                    Location location = (Location) msg.obj;
                    //mMap.put(0, new String[]{"Lat: ", "" + location.getLatitude()});
                    mMap.put(0, new String[]{"Lat: ", String.format("%3.8f",location.getLatitude())});
                    mMap.put(1, new String[]{"Accuracy: ", "" + location.getAccuracy()});
                    //mMap.put(2, new String[]{"Lon: ", "" + location.getLongitude()});
                    mMap.put(2, new String[]{"Lon: ", String.format("%3.8f",location.getLongitude())});
                    mMap.put(3, new String[]{"Speed: ", "" + location.getSpeed()});
                    mMap.put(4, new String[]{"Alt: ", "" + location.getAltitude()});
                    mMap.put(5, new String[]{"Bearing: ", "" + location.getBearing()});
                    //mUtcTime.setText("UTC Time: " + FileUtils.formatTime(location.getTime()));
                    if (mAdapter != null) {
                        mAdapter.notifyDataSetChanged();
                    }
                    break;
                case GPS_STATUS:
                    GpsStatus gpsStatus = (GpsStatus) msg.obj;
                    if (gpsStatus == null) {
                        return;
                    }

                    mMap.put(7, new String[]{"TTFF: ", "" + String.format("%3.2f", (float)gpsStatus.getTimeToFirstFix()/1000)});
                    if (mAdapter != null) {
                        mAdapter.notifyDataSetChanged();
                    }
                    break;

                case GPS_NMEA_GPGGA:
                    String gpgga = (String) msg.obj;
                    String[] gpggas = gpgga.split(",");
                    mMap.put(6, new String[]{"In Use: ", "" + gpggas[7]});
                    mUtcTime.setText("UTC Time: " + gpggas[1]);
                    if (mAdapter != null) {
                        mAdapter.notifyDataSetChanged();
                    }
                    break;

                case GPS_NMEA_GPGSA:
                    String gpgsa = (String) msg.obj;
                    String[] gpgsas = gpgsa.split(",");
                    mMap.put(9, new String[]{"HDOP: ", "" + gpgsas[16]});
                    if (mAdapter != null) {
                        mAdapter.notifyDataSetChanged();
                    }
                    break;

                case GPS_NMEA_GPGSV:
                    String gpgsv = (String) msg.obj;
                    String[] gpgsvs = gpgsv.split(",");
                    mMap.put(8, new String[]{"In View: ", "" + gpgsvs[3]});
                    if (mAdapter != null) {
                        mAdapter.notifyDataSetChanged();
                    }
                    break;
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_tracking, null);
        mLocationManager = (LocationManager) getActivity().getSystemService(Context.LOCATION_SERVICE);
        mGridView = (GridView) view.findViewById(R.id.grid_view);
        mUtcTime = (TextView) view.findViewById(R.id.tv_utc_time);

        mStart = (Button) view.findViewById(R.id.btn_start);
        mStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!GpsUtils.checkGps(getActivity())) {
                    return;
                }
                mStart.setEnabled(false);
                mStop.setEnabled(true);
                intent = new Intent(getActivity(), TrackingService.class);
                getActivity().startService(intent);
            }
        });
        mStop = (Button) view.findViewById(R.id.btn_stop);
        mStop.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mStart.setEnabled(true);
                mStop.setEnabled(false);
                intent = new Intent(getActivity(), TrackingService.class);
                getActivity().stopService(intent);

            }
        });

        view.findViewById(R.id.btn_delete_xtra).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Bundle bundle = new Bundle();
                bundle.putBoolean("all", true);
                String strCommand = "delete_aiding_data";
                mLocationManager.sendExtraCommand(LocationManager.GPS_PROVIDER, strCommand, bundle);
            }
        });
        boolean trackServiceWork = FileUtils.isTrackServiceWork(getActivity());
        boolean ttffServiceWork = FileUtils.isTtffServiceWork(getActivity());
        Log.i(TAG,"trackServiceWork = " + trackServiceWork + ", ttffServiceWork = " + ttffServiceWork);
        if (trackServiceWork){
            mStop.setEnabled(true);
            mStart.setEnabled(false);
        }
        if (ttffServiceWork){
            mStop.setEnabled(false);
            mStart.setEnabled(false);
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
        mMap.put(3, new String[]{"Speed: ", ""});
        mMap.put(4, new String[]{"Alt: ", ""});
        mMap.put(5, new String[]{"Bearing: ", ""});
        mMap.put(6, new String[]{"In Use: ", ""});
        mMap.put(7, new String[]{"TTFF: ", ""});
        mMap.put(8, new String[]{"In View: ", ""});
        mMap.put(9, new String[]{"HDOP: ", ""});
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

}