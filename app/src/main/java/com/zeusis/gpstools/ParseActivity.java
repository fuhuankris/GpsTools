package com.zeusis.gpstools;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.zeusis.gpstools.utils.NmeaParse;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class ParseActivity extends Activity implements View.OnClickListener {

    private NmeaParse mNmeaParse;
    private Map<String, List<NmeaParse.SatelliteInfo>> mMap;
    private List<String> mFileNames;
    private ListView mListView;
    private MyAdapter mAdapter;
    private LinearLayout mContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_parse);
        mNmeaParse = new NmeaParse();
        mMap = new HashMap<>();
        mFileNames = new ArrayList<>();
        findViewById(R.id.btn_parse).setOnClickListener(this);
        //mListView = (ListView) findViewById(R.id.list_view);
        mContent = (LinearLayout) findViewById(R.id.ll_content);
    }

    @Override
    public void onClick(View v) {
        mFileNames = mNmeaParse.getFileLists(this);
        String[] items = new String[mFileNames.size()];
        for (int i = 0; i < mFileNames.size(); i++) {
            items[i] = mFileNames.get(i);
        }
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setItems(items, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                mMap.clear();
                File file = new File(mFileNames.get(which));
                mMap = mNmeaParse.nmeaParse(file);
                updateView();
            }
        }).setTitle("选择文件").show();
    }

    private void updateView() {
        if (mMap == null) {
            return;
        }
        mContent.removeAllViews();
        /*if (mAdapter == null){
            mAdapter = new MyAdapter();
            mListView.setAdapter(mAdapter);
        }else {
            mAdapter.notifyDataSetChanged();
        }*/


        Set<Map.Entry<String, List<NmeaParse.SatelliteInfo>>> entries = mMap.entrySet();
        Iterator<Map.Entry<String, List<NmeaParse.SatelliteInfo>>> iterator = entries.iterator();
        while (iterator.hasNext()) {
            View view = View.inflate(ParseActivity.this, R.layout.item_parse, null);
            TextView tvSnr = (TextView) view.findViewById(R.id.tv_snr);
            TextView tvTotal = (TextView) view.findViewById(R.id.tv_total);
            TextView tvPnr = (TextView) view.findViewById(R.id.tv_pnr);
            Map.Entry<String, List<NmeaParse.SatelliteInfo>> next = iterator.next();
            tvPnr.setText("prn =" +  next.getKey());
            tvTotal.setText("total = " + next.getValue().size());
            tvSnr.setText("snr = " + getSnr(next.getValue()));
            view.setPadding(10,10,10,10);
            mContent.addView(view);
        }

    }

    private class MyAdapter extends BaseAdapter {
        @Override
        public int getCount() {
            return mMap.size();
        }

        @Override
        public Object getItem(int position) {
            return null;
        }

        @Override
        public long getItemId(int position) {
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = View.inflate(ParseActivity.this, R.layout.item_parse, null);
            }
            TextView tvSnr = (TextView) convertView.findViewById(R.id.tv_snr);
            TextView tvTotal = (TextView) convertView.findViewById(R.id.tv_total);
            TextView tvPnr = (TextView) convertView.findViewById(R.id.tv_pnr);
            Set<Map.Entry<String, List<NmeaParse.SatelliteInfo>>> entries = mMap.entrySet();
            Iterator<Map.Entry<String, List<NmeaParse.SatelliteInfo>>> iterator = entries.iterator();
            while (iterator.hasNext()) {
                Map.Entry<String, List<NmeaParse.SatelliteInfo>> next = iterator.next();
                tvPnr.setText(next.getKey());
                tvTotal.setText("" + next.getValue().size());
                tvSnr.setText(getSnr(next.getValue()));
            }
            return convertView;
        }
    }

    private String getSnr(List<NmeaParse.SatelliteInfo> value) {
        List<Integer> list = new ArrayList<>();
        for (int i = 0; i < value.size(); i++) {
            String srn = value.get(i).srn;
            try {
                int parseInt = Integer.parseInt(srn);
                list.add(parseInt);
            } catch (NumberFormatException e) {
                e.printStackTrace();
                list.add(0);
            }
        }
        int sum = 0;
        for (int i = 0; i < list.size(); i++) {
            sum += list.get(i);
        }
        if (list.size() == 0) {
            return "0";
        }
        return String.valueOf(sum / list.size());
    }
}
