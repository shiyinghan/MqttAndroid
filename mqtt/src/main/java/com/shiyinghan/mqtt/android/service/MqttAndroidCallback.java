package com.shiyinghan.mqtt.android.service;

import android.os.Handler;
import android.os.Looper;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttMessage;

class MqttAndroidCallback implements MqttCallbackExtended {
    private static final String TAG = MqttAndroidCallback.class.getSimpleName();

    private MqttCallback callback;

    /**
     * Make sure MqttCallback to run on the  Android Thread
     */
    private Handler handler;

    public MqttAndroidCallback(MqttCallback callback) {
        this.callback = callback;

        if (Looper.myLooper() != null) {
            handler = new Handler(Looper.myLooper());
        } else {
            handler = new Handler(Looper.getMainLooper());
        }
    }

    @Override
    public void connectComplete(boolean reconnect, String serverURI) {
        handler.post(() -> {
            if (callback instanceof MqttCallbackExtended) {
                ((MqttCallbackExtended) callback).connectComplete(reconnect, serverURI);
            }
        });
    }

    @Override
    public void connectionLost(Throwable cause) {
        handler.post(() -> {
            callback.connectionLost(cause);
        });
    }

    @Override
    public void messageArrived(String topic, MqttMessage message) throws Exception {
        handler.post(() -> {
            try {
                callback.messageArrived(topic, message);
            } catch (Exception e) {
                Log.e(TAG, e.getMessage(), e);
            }
        });
    }

    @Override
    public void deliveryComplete(IMqttDeliveryToken token) {
        handler.post(() -> {
            callback.deliveryComplete(token);
        });
    }
}
