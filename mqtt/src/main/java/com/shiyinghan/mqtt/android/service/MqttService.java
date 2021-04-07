/*******************************************************************************
 * Copyright (c) 1999, 2016 IBM Corp.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * and Eclipse Distribution License v1.0 which accompany this distribution.
 *
 * The Eclipse Public License is available at
 *    http://www.eclipse.org/legal/epl-v10.html
 * and the Eclipse Distribution License is available at
 *   http://www.eclipse.org/org/documents/edl-v10.php.
 *
 * Contributors:
 *   James Sutton - isOnline Null Pointer (bug 473775)
 */
package com.shiyinghan.mqtt.android.service;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import android.annotation.SuppressLint;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

/**
 * <p>
 * The android service which interfaces with an MQTT client implementation
 * </p>
 * <p>
 * The main API of MqttService is intended to pretty much mirror the
 * IMqttAsyncClient with appropriate adjustments for the Android environment.<br>
 * These adjustments usually consist of adding two parameters to each method :-
 * </p>
 * <ul>
 * <li>invocationContext - a string passed from the application to identify the
 * context of the operation (mainly included for support of the javascript API
 * implementation)</li>
 * <li>activityToken - a string passed from the Activity to relate back to a
 * callback method or other context-specific data</li>
 * </ul>
 * <p>
 * To support multiple client connections, the bulk of the MQTT work is
 * delegated to MqttConnection objects. These are identified by "client
 * handle" strings, which is how the Activity, and the higher-level APIs refer
 * to them.
 * </p>
 * <p>
 * Activities using this service are expected to start it and bind to it using
 * the BIND_AUTO_CREATE flag. The life cycle of this service is based on this
 * approach.
 * </p>
 */
@SuppressLint("Registered")
public class MqttService extends Service {

    // Identifier for Intents, log messages, etc..
    protected static final String TAG = MqttService.class.getSimpleName();

    // somewhere to persist received messages until we're sure
    // that they've reached the application
    private MessageStore messageStore;

    public MessageStore getMessageStore() {
        return messageStore;
    }

    // An intent receiver to deal with changes in network connectivity
    private NetworkConnectionIntentReceiver networkConnectionMonitor;

    // a way to pass ourself back to the activity
    private MqttServiceBinder mqttServiceBinder;

    /**
     * mapping from client handle strings to actual client.
     */
    private static final Map<String, MqttAndroidClient> clientMap = new ConcurrentHashMap<>();

    public MqttService() {
        super();
    }

    // Extend Service

    /**
     * @see android.app.Service#onCreate()
     */
    @Override
    public void onCreate() {
        Log.d(TAG, "onCreate");
        super.onCreate();

        // create a binder that will let the Activity UI send
        // commands to the Service
        mqttServiceBinder = new MqttServiceBinder(this);

        // create somewhere to buffer received messages until
        // we know that they have been passed to the application
        messageStore = new DatabaseMessageStore(this);
    }

    /**
     * @see android.app.Service#onBind(Intent)
     */
    @Override
    public IBinder onBind(Intent intent) {
        Log.d(TAG, "onBind");
        return mqttServiceBinder;
    }

    /**
     * @see android.app.Service#onStartCommand(Intent, int, int)
     */
    @Override
    public int onStartCommand(final Intent intent, int flags, final int startId) {
        Log.d(TAG, "onStartCommand");

        // run till explicitly stopped, restart when
        // process restarted
        registerBroadcastReceivers();

        return START_STICKY;
    }

    /**
     * @see android.app.Service#onDestroy()
     */
    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");

        // clear down
        clear();

        if (mqttServiceBinder != null) {
            mqttServiceBinder = null;
        }

        unregisterBroadcastReceivers();

        if (this.messageStore != null) {
            this.messageStore.close();
        }

        super.onDestroy();
    }

    public static void putClient(String clientHandle, MqttAndroidClient client) {
        if (!clientMap.containsKey(clientHandle)) {
            clientMap.put(clientHandle, client);
        }
    }

    /**
     * Disconnect and remove all clients
     */
    public static void clear() {
        Log.d(TAG, "Clear exist client, client size=" + clientMap.size());
        try {
            for (MqttAndroidClient client : clientMap.values()) {
                client.disconnect();
            }
        } catch (Exception e) {
            Log.e(TAG, "Reconnect error:" + e.getLocalizedMessage(), e);
        }
        clientMap.clear();
    }

    /**
     * Called by the Activity when a message has been passed back to the
     * application
     *
     * @param clientHandle identifier for the client which received the message
     * @param id           identifier for the MQTT message
     * @return true -> OK ; false -> ERROR
     */
    public boolean acknowledgeMessageArrival(String clientHandle, String id) {
        return messageStore.discardArrived(clientHandle, id);
    }

    @SuppressWarnings("deprecation")
    private void registerBroadcastReceivers() {
        if (networkConnectionMonitor == null) {
            networkConnectionMonitor = new NetworkConnectionIntentReceiver();
            registerReceiver(networkConnectionMonitor, new IntentFilter(
                    ConnectivityManager.CONNECTIVITY_ACTION));
        }
    }

    private void unregisterBroadcastReceivers() {
        if (networkConnectionMonitor != null) {
            unregisterReceiver(networkConnectionMonitor);
            networkConnectionMonitor = null;
        }
    }

    /*
     * Called in response to a change in network connection - after losing a
     * connection to the server, this allows us to wait until we have a usable
     * data connection again
     */
    private class NetworkConnectionIntentReceiver extends BroadcastReceiver {

        @Override
        @SuppressLint("Wakelock")
        public void onReceive(Context context, Intent intent) {
            Log.d(TAG, "Internal network status receive.");
            // we protect against the phone switching off
            // by requesting a wake lock - we request the minimum possible wake
            // lock - just enough to keep the CPU running until we've finished
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            WakeLock wl = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "MQTT:TAG");
            wl.acquire();
            Log.d(TAG, "Reconnect for Network recovery.");
            if (isOnline()) {
                Log.d(TAG, "Online,reconnect.");
                // we have an internet connection - have another try at
                // connecting
                notifyClientsOnline();
            } else {
                notifyClientsOffline();
            }

            wl.release();
        }
    }

    /**
     * @return whether the android service can be regarded as online
     */
    public boolean isOnline() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = cm.getActiveNetworkInfo();
        //noinspection RedundantIfStatement
        if (networkInfo != null
                && networkInfo.isAvailable()
                && networkInfo.isConnected()) {
            return true;
        }

        return false;
    }

    /**
     * Notify clients we're online
     */
    public static void notifyClientsOnline() {
        Log.d(TAG, "Online, client size=" + clientMap.size());
        try {
            for (MqttAndroidClient client : clientMap.values()) {
                client.notifyOnline();
            }
        } catch (Exception e) {
            Log.e(TAG, "Online error:" + e.getLocalizedMessage(), e);
        }
    }

    /**
     * Notify clients we're offline
     */
    private static void notifyClientsOffline() {
        Log.d(TAG, "Offline, client size=" + clientMap.size());
        for (MqttAndroidClient client : clientMap.values()) {
            client.notifyOffline();
        }
    }

    static class MqttServiceBinder extends Binder {

        private MqttService mqttService;

        MqttServiceBinder(MqttService mqttService) {
            this.mqttService = mqttService;
        }

        /**
         * @return a reference to the Service
         */
        public MqttService getService() {
            return mqttService;
        }

    }
}
