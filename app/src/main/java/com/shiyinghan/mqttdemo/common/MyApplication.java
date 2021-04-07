package com.shiyinghan.mqttdemo.common;

import android.app.Application;

import androidx.multidex.MultiDex;

import com.shiyinghan.mqttdemo.reactivex.RxUtils;
import com.shiyinghan.mqttdemo.utils.NotifyUtil;
import com.shiyinghan.mqttdemo.utils.ToastUtil;

import me.jessyan.autosize.AutoSizeConfig;
import me.jessyan.autosize.unit.Subunits;

/**
 * 自定义Application类
 *
 * @author admin
 */
public class MyApplication extends Application {

    private static final String TAG = MyApplication.class.getSimpleName();

    private static MyApplication sInstance;

    @Override
    public void onCreate() {
        super.onCreate();

        sInstance = this;

        MultiDex.install(this);

        //停止对两个主单位(DP,SP)的支持,选择PT作为副单位
        AutoSizeConfig.getInstance().getUnitsManager()
                .setSupportDP(false)
                .setSupportSP(false)
                .setSupportSubunits(Subunits.PT);

        NotifyUtil.initNotificationChannel(this);

        ToastUtil.init(this);
    }

    @Override
    public void onTerminate() {
        super.onTerminate();

        RxUtils.release();
    }

    /**
     * 获取Application的实例
     *
     * @return MyApplication
     */
    public static MyApplication getInstance() {
        return sInstance;
    }
}
