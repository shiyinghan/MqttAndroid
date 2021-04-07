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
 */
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