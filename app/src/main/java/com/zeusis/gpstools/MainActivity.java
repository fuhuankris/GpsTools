package com.zeusis.gpstools;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.WindowManager;
import android.widget.RadioGroup;

import com.zeusis.gpstools.fragment.SettingFragment;
import com.zeusis.gpstools.fragment.TrackingTestFragment;
import com.zeusis.gpstools.fragment.TtffTestFragment;
import com.zeusis.gpstools.utils.Constant;
import com.zeusis.gpstools.utils.FileUtils;


public class MainActivity extends Activity {


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON, WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_main);
        RadioGroup radioGroup = (RadioGroup) findViewById(R.id.radio_group);
        String deviceModel = Build.MODEL.replace(" ","_");

        FileUtils.saveString2Sp(getApplication(), Constant.DEVICE_MODEL,deviceModel);

        radioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                switch (checkedId){
                    case R.id.rb_track:
                        getFragmentManager().beginTransaction().replace(R.id.content,new TrackingTestFragment()).commit();
                        break;
                    case R.id.rb_setting:
                        getFragmentManager().beginTransaction().replace(R.id.content,new SettingFragment()).commit();
                        break;
                    case R.id.rb_ttff:
                        getFragmentManager().beginTransaction().replace(R.id.content,new TtffTestFragment()).commit();
                }
            }
        });
        radioGroup.check(R.id.rb_setting);
    }
}
