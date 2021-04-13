package com.shiyinghan.mqtt.demo.utils;

import android.content.SharedPreferences;

import com.shiyinghan.mqtt.demo.common.Constant;
import com.shiyinghan.mqtt.demo.common.MyApplication;

/**
 * @author admin
 */
public class SharedPreferencesUtil {

    public static SharedPreferences getSharedPreferences() {
        return MyApplication.getInstance().getSharedPreferences(Constant.SP_FILE_NAME, 0);
    }
}
