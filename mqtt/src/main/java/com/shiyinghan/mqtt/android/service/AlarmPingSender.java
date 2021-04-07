package com.shiyinghan.mqtt.android.service;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Build;
import android.os.PowerManager;
import android.os.PowerManager.WakeLock;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttPingSender;
import org.eclipse.paho.client.mqttv3.internal.ClientComms;

/**
 * Default ping sender implementation on Android. It is based on AlarmManager.
 *
 * <p>This class implements the {@link MqttPingSender} pinger interface
 * allowing applications to send ping packet to server every keep alive interval.
 * </p>
 *
 * @see MqttPingSender
 */
class AlarmPingSender implements MqttPingSender {
    // Identifier for Intents, log messages, etc..
    private static final String TAG = AlarmPingSender.class.getSimpleName();

    // TODO: Add log.
    private ClientComms comms;
    private Context context;
    private BroadcastReceiver alarmReceiver;
    private AlarmPingSender that;
    private PendingIntent pendingIntent;
    private volatile boolean hasStarted = false;

    public AlarmPingSender(Context context) {
        if (context == null) {
            throw new IllegalArgumentException(
                    "Neither service nor client can be null.");
        }
        this.context = context;
        that = this;
    }

    @Override
    public void init(ClientComms comms) {
        this.comms = comms;
        this.alarmReceiver = new AlarmReceiver();
    }

    @Override
    public void start() {
        String action = MqttConstants.PING_SENDER
                + comms.getClient().getClientId();
        Log.d(TAG, "Register alarmreceiver to context " + action);
        context.registerReceiver(alarmReceiver, new IntentFilter(action));

        pendingIntent = PendingIntent.getBroadcast(context, 0, new Intent(
                action), PendingIntent.FLAG_UPDATE_CURRENT);

        schedule(comms.getKeepAlive());
        hasStarted = true;
    }

    @Override
    public void stop() {

        Log.d(TAG, "Unregister alarmreceiver to context " + comms.getClient().getClientId());
        if (hasStarted) {
            if (pendingIntent != null) {
                // Cancel Alarm.
                AlarmManager alarmManager = (AlarmManager) context.getSystemService(Service.ALARM_SERVICE);
                alarmManager.cancel(pendingIntent);
            }

            hasStarted = false;
            try {
                context.unregisterReceiver(alarmReceiver);
            } catch (IllegalArgumentException e) {
                //Ignore unregister errors.
            }
        }
    }

    @Override
    public void schedule(long delayInMilliseconds) {
        long nextAlarmInMilliseconds = System.currentTimeMillis()
                + delayInMilliseconds;
        Log.d(TAG, "Schedule next alarm at " + nextAlarmInMilliseconds);
        AlarmManager alarmManager = (AlarmManager) context
                .getSystemService(Service.ALARM_SERVICE);

        //MARSHMALLOW 23 OR ABOVE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            // In SDK 23 and above, dosing will prevent setExact, setExactAndAllowWhileIdle will force
            // the device to run this task whitelist dosing.
            Log.d(TAG, "Alarm scheule using setExactAndAllowWhileIdle, next: " + delayInMilliseconds);
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextAlarmInMilliseconds,
                    pendingIntent);
        }
        //LOLLIPOP 21 OR ABOVE
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Log.d(TAG, "Alarm scheule using setAlarmClock, next: " + delayInMilliseconds);
            AlarmManager.AlarmClockInfo alarmClockInfo = new AlarmManager.AlarmClockInfo(nextAlarmInMilliseconds, pendingIntent);
            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent);
        }
        //KITKAT 19 OR ABOVE
        else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            Log.d(TAG, "Alarm scheule using setExact, delay: " + delayInMilliseconds);
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, nextAlarmInMilliseconds,
                    pendingIntent);
        }
        //FOR BELOW KITKAT ALL DEVICES
        else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, nextAlarmInMilliseconds,
                    pendingIntent);
        }
    }

    /*
     * This class sends PingReq packet to MQTT broker
     */
    class AlarmReceiver extends BroadcastReceiver {
        private WakeLock wakelock;
        private final String wakeLockTag = MqttConstants.PING_WAKELOCK
                + that.comms.getClient().getClientId();

        @Override
        @SuppressLint("Wakelock")
        public void onReceive(Context context, Intent intent) {
            // According to the docs, "Alarm Manager holds a CPU wake lock as
            // long as the alarm receiver's onReceive() method is executing.
            // This guarantees that the phone will not sleep until you have
            // finished handling the broadcast.", but this class still get
            // a wake lock to wait for ping finished.

            Log.d(TAG, "Sending Ping at:" + System.currentTimeMillis());

            PowerManager pm = (PowerManager) AlarmPingSender.this.context
                    .getSystemService(Service.POWER_SERVICE);
            wakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, wakeLockTag);
            wakelock.acquire();

            // Assign new callback to token to execute code after PingResq
            // arrives. Get another wakelock even receiver already has one,
            // release it until ping response returns.
            IMqttToken token = comms.checkForActivity(new IMqttActionListener() {

                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d(TAG, "Success. Release lock(" + wakeLockTag + "):"
                            + System.currentTimeMillis());
                    //Release wakelock when it is done.
                    wakelock.release();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken,
                                      Throwable exception) {
                    Log.d(TAG, "Failure. Release lock(" + wakeLockTag + "):"
                            + System.currentTimeMillis());
                    //Release wakelock when it is done.
                    wakelock.release();
                }
            });


            if (token == null && wakelock.isHeld()) {
                wakelock.release();
            }
        }
    }
}
