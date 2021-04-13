package com.shiyinghan.mqtt.demo.reactivex.observer;


import android.util.Log;

import io.reactivex.observers.DisposableObserver;

/**
 * @author admin
 */
public abstract class BaseHandleObserver<T> extends DisposableObserver<T> {
    private static final String TAG = "BaseHandleObserver";

    @Override
    public void onComplete() {
        Log.e(TAG, "onComplete");
    }

    @Override
    public void onError(Throwable e) {
        if (e.getMessage() == null) {
            e.printStackTrace();
        } else {
            Log.e(TAG, e.getMessage());
        }
    }
}
