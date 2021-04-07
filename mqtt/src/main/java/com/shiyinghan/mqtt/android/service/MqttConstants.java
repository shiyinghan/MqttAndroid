package com.shiyinghan.mqtt.android.service;

/**
 * Various strings used to identify operations or data in the Android MQTT
 * service, mainly used in Intents passed between Activities and the Service.
 */
public final class MqttConstants {

    /**
     * Attributes of messages <p> Used for the column names in the database
     */
    public static final String DUPLICATE = "duplicate";
    public static final String RETAINED = "retained";
    public static final String QOS = "qos";
    public static final String PAYLOAD = "payload";
    public static final String DESTINATION_NAME = "destinationName";
    public static final String CLIENT_HANDLE = "clientHandle";
    public static final String MESSAGE_ID = "messageId";

    /**
     * Intent prefix for Ping sender.
     */
    public static final String PING_SENDER = MqttService.TAG + ".pingSender.";

    /**
     * Constant for wakelock
     */
    public static final String PING_WAKELOCK = MqttService.TAG + ".client.";
}