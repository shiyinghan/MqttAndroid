package com.shiyinghan.mqtt.android.service;

import android.util.Log;

/**
 * Default simple trace callback.
 */
public class MqttTraceCallback implements MqttTraceHandler {

    @Override
    public void traceDebug(String arg0, String arg1) {
        Log.i(arg0, arg1);
    }

    @Override
    public void traceError(String arg0, String arg1) {
        Log.e(arg0, arg1);
    }

    @Override
    public void traceException(String arg0, String arg1, Exception arg2) {
        Log.e(arg0, arg1, arg2);
    }
}
