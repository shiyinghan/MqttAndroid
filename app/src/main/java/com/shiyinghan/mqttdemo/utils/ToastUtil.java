package com.shiyinghan.mqttdemo.utils;

import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import java.lang.ref.WeakReference;


public class ToastUtil {
    private static final String TAG = ToastUtil.class.getSimpleName();

    private static WeakReference<Toast> sToast;
    private static Context sContext;
    private static Handler sHandler;

    public static void init(Context context) {
        sContext = context;
        sHandler = new Handler(Looper.getMainLooper());
    }

    private static void showToastInMainThread(CharSequence content) {
        cancel();
        try {
            Toast toast = Toast.makeText(sContext, content, Toast.LENGTH_SHORT);
            sToast = new WeakReference<>(toast);
            toast.show();
        } catch (Exception e) {
            Log.e(TAG, e.getLocalizedMessage(), e);
            //解决在子线程中调用Toast的异常情况处理
            Looper.prepare();
            Toast.makeText(sContext, content, Toast.LENGTH_SHORT).show();
            Looper.loop();
        }
    }

    public static void show(CharSequence content) {
        if (sContext == null) {
            return;
        }
        if (TextUtils.isEmpty(content)) {
            return;
        }
        sHandler.post(() -> showToastInMainThread(content));
    }

    public static void cancel() {
        if (sToast != null && sToast.get() != null) {
            sToast.get().cancel();
        }
    }
}
