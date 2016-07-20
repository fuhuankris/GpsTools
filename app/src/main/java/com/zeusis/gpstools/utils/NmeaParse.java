package com.zeusis.gpstools.utils;

import android.content.Context;
import android.os.Environment;
import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by kris.fu on 2016/5/24.
 */
public class NmeaParse {

    public Map<String, List<SatelliteInfo>> nmeaParse(File file) {
        Map<String, List<SatelliteInfo>> map = new HashMap<>();
        if (file == null) {
            return null;
        }
        if (!file.exists()) {
            return null;
        }

        try {
            BufferedReader br = new BufferedReader(new FileReader(file));
            String line = null;
            while ((line = br.readLine()) != null) {
                if (line.contains("GSV")) {
                    parseLine(map, line);
                }
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
            return null;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return map;
    }

    private void parseLine(Map<String, List<SatelliteInfo>> map, String line) {
        String s = line.replace("*", ",");
        String[] strings = s.split(",");
        try {
            String satellite1 = strings[4];
            if (!TextUtils.isEmpty(satellite1)) {
                SatelliteInfo info = new SatelliteInfo();
                info.elevation = strings[5];
                info.azimuth = strings[6];
                info.srn = strings[7];
                List<SatelliteInfo> satelliteInfos = map.get(satellite1);
                if (satelliteInfos == null) {
                    satelliteInfos = new ArrayList<SatelliteInfo>();
                    satelliteInfos.add(info);
                    map.put(satellite1, satelliteInfos);
                } else {
                    satelliteInfos.add(info);
                }
            }

            String satellite2 = strings[8];
            if (!TextUtils.isEmpty(satellite2)) {
                SatelliteInfo info = new SatelliteInfo();
                info.elevation = strings[9];
                info.azimuth = strings[10];
                info.srn = strings[11];
                List<SatelliteInfo> satelliteInfos = map.get(satellite2);
                if (satelliteInfos == null) {
                    satelliteInfos = new ArrayList<SatelliteInfo>();
                    satelliteInfos.add(info);
                    map.put(satellite2, satelliteInfos);
                } else {
                    satelliteInfos.add(info);
                }
            }

            String satellite3 = strings[12];
            if (!TextUtils.isEmpty(satellite3)) {
                SatelliteInfo info = new SatelliteInfo();
                info.elevation = strings[13];
                info.azimuth = strings[14];
                info.srn = strings[15];
                List<SatelliteInfo> satelliteInfos = map.get(satellite3);
                if (satelliteInfos == null) {
                    satelliteInfos = new ArrayList<SatelliteInfo>();
                    satelliteInfos.add(info);
                    map.put(satellite3, satelliteInfos);
                } else {
                    satelliteInfos.add(info);
                }
            }

            String satellite4 = strings[16];
            if (!TextUtils.isEmpty(satellite4)) {
                SatelliteInfo info = new SatelliteInfo();
                info.elevation = strings[17];
                info.azimuth = strings[18];
                info.srn = strings[19];
                List<SatelliteInfo> satelliteInfos = map.get(satellite4);
                if (satelliteInfos == null) {
                    satelliteInfos = new ArrayList<SatelliteInfo>();
                    satelliteInfos.add(info);
                    map.put(satellite4, satelliteInfos);
                } else {
                    satelliteInfos.add(info);
                }
            }
        } catch (IndexOutOfBoundsException e) {
            e.printStackTrace();
            return;
        }

    }

    public List<String> getFileLists(Context context) {
        List<String> list = new ArrayList<>();
        File file = FileUtils.getRootFile(context);
        if (!file.exists() || !file.isDirectory()) {
            return null;
        }
        getFileName(list, file);
        return list;
    }

    private void getFileName(List<String> list, File file) {
        File[] files = file.listFiles();
        for (File f : files) {
            if (f.isDirectory()) {
                getFileName(list, f);
            } else {
                list.add(f.getAbsolutePath());
            }
        }
    }

    public class SatelliteInfo {
        /**
         * 卫星仰角
         */
        public String elevation;
        /**
         * 卫星方位角
         */
        public String azimuth;
        /**
         * 信噪比
         */
        public String srn;
    }
}
