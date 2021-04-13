package com.shiyinghan.mqtt.demo.mqtt.entity;

import org.eclipse.paho.client.mqttv3.MqttMessage;

import java.util.Date;

public class ReceivedMessageEntity {
    private final String clientId;
    private final String topic;
    private final MqttMessage message;
    private final Date timestamp;

    public ReceivedMessageEntity(String clientId, String topic, MqttMessage message) {
        this.clientId = clientId;
        this.topic = topic;
        this.message = message;
        this.timestamp = new Date();
    }

    public String getClientId() {
        return clientId;
    }

    public String getTopic() {
        return topic;
    }

    public MqttMessage getMessage() {
        return message;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    @Override
    public String toString() {
        return "ReceivedMessageEntity{" +
                "clientId='" + clientId + '\'' +
                "topic='" + topic + '\'' +
                ", message='" + message + '\'' +
                ", timestamp=" + timestamp +
                '}';
    }
}
