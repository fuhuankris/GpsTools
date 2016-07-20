package com.zeusis.gpstools.utils;

import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.text.TextUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by fuhuan on 2016/4/26.
 */
public class FileUtils {

    private static final String GPS_TEST = "gps";
    private static final String GPS_TRACK = "track";
    private static final String GPS_TTFF = "ttff";

    public static File getFile(Context context, String type) {
        String editStr = getFileName(context);
        File fileDir = new File(Environment.getExternalStorageDirectory().getPath() + File.separator + "gps");
        if (!fileDir.exists()) {
            fileDir.mkdir();
        }

        File file = new File(fileDir, getStringFromSp(context, Constant.DEVICE_MODEL) + editStr + type + formatTime(System.currentTimeMillis()) + ".txt");

        return file;
    }

    public static File getRootFile(Context context) {
        File rootDir = new File(Environment.getExternalStorageDirectory().getPath() + File.separator + "gps");
        if (!rootDir.exists()) {
            rootDir.mkdir();
        }
        File rootDirc = new File(rootDir.getPath() + File.separator + "zeusis");
        if (!rootDirc.exists()) {
            rootDirc.mkdir();
        }
        return rootDirc;
    }

    public static File getTrackFile(Context context, String type) {
        String editStr = getFileName(context);
        File rootFile = getRootFile(context);
        File fileDir = new File(rootFile, GPS_TRACK);
        if (!fileDir.exists()) {
            fileDir.mkdir();
        }
        File file = new File(fileDir, getStringFromSp(context, Constant.DEVICE_MODEL) + editStr + type + formatTime(System.currentTimeMillis()) + ".txt");
        return file;
    }

    public static File getTrackFile(Context context, String type, String suffix) {
        String editStr = getFileName(context);
        File rootFile = getRootFile(context);
        File fileDir = new File(rootFile, GPS_TRACK);
        if (!fileDir.exists()) {
            fileDir.mkdir();
        }
        File file = new File(fileDir, getStringFromSp(context, Constant.DEVICE_MODEL) + editStr + type + formatTime(System.currentTimeMillis()) + suffix);
        return file;
    }

    public static File getTtffFile(Context context, String type, String suffix) {
        String editStr = getFileName(context);
        File rootFile = getRootFile(context);
        File fileDir = new File(rootFile, GPS_TTFF);
        if (!fileDir.exists()) {
            fileDir.mkdir();
        }
        File file = new File(fileDir, getStringFromSp(context, Constant.DEVICE_MODEL) + editStr + type + formatTime(System.currentTimeMillis()) + suffix);
        return file;
    }

    private static String getFileName(Context context) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        return prefs.getString("log_file_name", "");
    }

    public static String getStringFromSp(Context context, String key) {
        return context.getSharedPreferences(GPS_TEST, Context.MODE_PRIVATE).getString(key, "");
    }

    public static void saveString2Sp(Context context, String key, String value) {
        context.getSharedPreferences(GPS_TEST, Context.MODE_PRIVATE).edit().putString(key, value).commit();
    }

    public static String formatTime(long time) {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmmss_SSS");
        return sdf.format(new Date(time));
    }

    public static String formatTimeForFile(long time) {
        SimpleDateFormat sdf = new SimpleDateFormat("HH_mm_ss");
        return sdf.format(new Date(time));
    }

    public static void closeFos(FileOutputStream fos) {
        try {
            if (fos != null) {
                fos.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String formatNum2Time(int num) {
        int min = num / 60;
        int sec = num % 60;
        String minStr = min < 10 ? "0" + min : "" + min;
        String secStr = sec < 10 ? "0" + sec : "" + sec;
        return minStr + ":" + secStr;
    }

    private static boolean isServiceWork(Context mContext, String serviceName) {

        boolean isWork = false;
        ActivityManager myAM = (ActivityManager) mContext.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningServiceInfo> myList = (List<ActivityManager.RunningServiceInfo>) myAM.getRunningServices(400);
        if (myList.size() <= 0) {
            return false;
        }
        for (int i = 0; i < myList.size(); i++) {
            String mName = myList.get(i).service.getClassName().toString();
            if (mName.equals(serviceName)) {
                isWork = true;
                break;
            }
        }
        return isWork;
    }

    public static boolean isTrackServiceWork(Context context){
        return isServiceWork(context,Constant.TRACK_SERVICE);
    }

    public static boolean isTtffServiceWork(Context context){
        return isServiceWork(context,Constant.TTFF_SERVICE);
    }

}
