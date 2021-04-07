package com.shiyinghan.mqttdemo.utils;

import android.content.SharedPreferences;

import com.shiyinghan.mqttdemo.common.Constant;
import com.shiyinghan.mqttdemo.common.MyApplication;

/**
 * @author admin
 */
public class SharedPreferencesUtil {

    public static SharedPreferences getSharedPreferences() {
        return MyApplication.getInstance().getSharedPreferences(Constant.SP_FILE_NAME, 0);
    }
}
