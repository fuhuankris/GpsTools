package com.zeusis.gpstools.utils;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.location.LocationManager;
import android.provider.Settings;

import java.util.Arrays;
import java.util.List;

/**
 * Created by fuhuan on 2016/4/25.
 */
public class GpsUtils {

    private static final double EARTH_RADIUS = 6378.137;

    private static double rad(double d) {
        return d * Math.PI / 180.0;
    }

    public static double GetDistance(double lat1, double lng1, double lat2, double lng2) {
        double radLat1 = rad(lat1);
        double radLat2 = rad(lat2);
        double a = radLat1 - radLat2;
        double b = rad(lng1) - rad(lng2);
        double s = 2 * Math.asin(Math.sqrt(Math.pow(Math.sin(a / 2), 2) + Math.cos(radLat1) * Math.cos(radLat2) * Math.pow(Math.sin(b / 2), 2)));
        s = s * EARTH_RADIUS;
        s = Math.round(s * 10000) / 10000;
        return s;
    }

    public static double getDistance(double lat1, double lon1, double lat2, double lon2) {
        double R = 6371;
        double distance = 0.0;
        double dLat = (lat2 - lat1) * Math.PI / 180;
        double dLon = (lon2 - lon1) * Math.PI / 180;
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(lat1 * Math.PI / 180)
                * Math.cos(lat2 * Math.PI / 180) * Math.sin(dLon / 2)
                * Math.sin(dLon / 2);
        distance = (2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))) * R;
        return distance * 1000;
    }

    public static boolean checkGps(Context context) {
        LocationManager locationManager = (LocationManager) context.
                getSystemService(Context.LOCATION_SERVICE);
        boolean isGpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        if (!isGpsEnabled) {
            Intent intent = new Intent();
            intent.setAction(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            try {
                context.startActivity(intent);
            } catch (ActivityNotFoundException ex) {
                intent.setAction(Settings.ACTION_SETTINGS);
                try {
                    context.startActivity(intent);
                } catch (Exception e) {
                }
            }
        }
        return isGpsEnabled;
    }

    public static double average(List<Double> arrays) {
        if (arrays == null || arrays.size() == 0) {
            return 0;
        }
        return sum(arrays) / arrays.size();
    }

    public static double square(double d) {
        return d * d;
    }

    public static double sum(List<Double> arrays) {
        double sum = 0;
        for (int i = 0; i < arrays.size(); i++) {
            sum += arrays.get(i);
        }
        return sum;
    }

    public static double standardDeviation(List<Double> ds, double u) {
        if (ds == null || ds.size() < 2) {
            return 0;
        }
        //double Ux = average(ds);
        double Ux = u;
        double sum = 0;
        for (int i = 0; i < ds.size(); i++) {
            sum += square(u - ds.get(i));
        }
        return Math.sqrt(sum / ds.size() - 1);
    }

    /**
     * 计算CEP值
     *
     * @param CEP CEP参数，如50，95，99
     * @param x   第一组参数
     * @param y   第二组参数 没有的时候传null
     * @return 对应的CEP值
     */
    public static double CEP(int CEP, List<Double> x, List<Double> y, double ux, double uy) {
        double ratio = 1;
        switch (CEP) {
            case 50:
                ratio = 0.8326;
                break;
            case 95:
                ratio = 1.2272;
                break;
            case 99:
                ratio = 1.5222;
                break;
            default:
                break;
        }
        if (x == null) {
            return 0;
        }
        double Ox = 0;
        double Oy = 0;
        if (y == null) {
            Oy = 0;
        } else {
            Oy = standardDeviation(y, uy);
        }
        Ox = standardDeviation(x, ux);
        return ratio * (Ox + Oy);
    }

    /**
     *
     * @param x double类型的集合
     * @param CEP CEP值
     * @return
     */
    public static double CEP(List<Double> x ,int CEP){
        if (x == null){
            return 0;
        }
        double cep = CEP * 0.01d;
        int next = (int)(x.size() * cep);
        int last = next -1;
        if (last <0){
            return x.get(next);
        }
        return x.get(last) + (x.get(next) - x.get(last)) * (x.size() * cep - next);
    }

}
