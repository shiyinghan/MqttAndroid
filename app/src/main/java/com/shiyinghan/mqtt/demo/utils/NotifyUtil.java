package com.shiyinghan.mqtt.demo.utils;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Build;

import androidx.core.app.NotificationCompat;

import com.shiyinghan.mqtt.demo.R;


/**
 * Provides static methods for creating and showing notifications to the user.
 */
public class NotifyUtil {

    /**
     * 通知渠道的id
     */
    private static String sNotificationChannelId = "mqtttest_android_push";

    /**
     * Message ID Counter
     **/
    private static int sMessageID = 0;

    /**
     * Android 8.0 以上设备，注册NotificationChannel
     *
     * @param context
     */
    public static void initNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            CharSequence name = context.getString(R.string.notification_channel_name);
            String description = context.getString(R.string.notification_channel_description);
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel mChannel = new NotificationChannel(sNotificationChannelId, name, importance);
            mChannel.setDescription(description);
            mChannel.enableLights(true);
            mChannel.setLightColor(Color.YELLOW);

            mChannel.enableVibration(false);
            mChannel.setVibrationPattern(new long[]{0});

            mChannel.setSound(null, null);

            mChannel.setLockscreenVisibility(Notification.VISIBILITY_PUBLIC);

            mNotificationManager.createNotificationChannel(mChannel);
        }
    }

    /**
     * Displays a notification in the notification area of the UI
     *
     * @param context           Context from which to create the notification
     * @param messageString     The string to display to the user as a message
     * @param intent            The intent which will start the activity when the user clicks the notification
     * @param notificationTitle The resource reference to the notification title
     */
    public static void showNotification(Context context, String messageString, Intent intent, int notificationTitle) {

        //Get the notification manage which we will use to display the notification
        String ns = Context.NOTIFICATION_SERVICE;
        NotificationManager mNotificationManager = (NotificationManager) context.getSystemService(ns);

        long when = System.currentTimeMillis();

        //get the notification title from the application's strings.xml file
        CharSequence contentTitle = context.getString(notificationTitle);

        //the message that will be displayed as the ticker
        String ticker = contentTitle + " " + messageString;

        //build the pending intent that will start the appropriate activity
        PendingIntent pendingIntent = PendingIntent.getActivity(context,
                0, intent, 0);

        //build the notification
        NotificationCompat.Builder notificationCompat = new NotificationCompat.Builder(context, sNotificationChannelId);
        notificationCompat.setAutoCancel(true)
                .setContentTitle(contentTitle)
                .setContentIntent(pendingIntent)
                .setContentText(messageString)
                .setTicker(ticker)
                .setWhen(when)
                .setSmallIcon(R.mipmap.ic_launcher);

        Notification notification = notificationCompat.build();
        //display the notification
        mNotificationManager.notify(sMessageID, notification);
        sMessageID++;

    }

}
