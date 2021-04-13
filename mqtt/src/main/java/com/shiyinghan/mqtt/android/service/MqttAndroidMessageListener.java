package com.shiyinghan.mqtt.android.service;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.MqttMessage;

class MqttAndroidMessageListener implements IMqttMessageListener {
    private static final String TAG = MqttAndroidMessageListener.class.getSimpleName();

    private IMqttMessageListener listener;

    /**
     * Make sure MqttMessageListener to run on the  Android Thread
     */
    private Handler handler;

    public MqttAndroidMessageListener(IMqttMessageListener listener) {
        this.listener = listener;

        if (Looper.myLooper() != null) {
            handler = new Handler(Looper.myLooper());
        } else {
            handler = new Handler(Looper.getMainLooper());
        }
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        handler.post(() -> {
            try {
                listener.messageArrived(topic, message);
            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
            }
        });
    }
}
