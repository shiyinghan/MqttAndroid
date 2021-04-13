package com.shiyinghan.mqtt.demo.mqtt.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity
public class SubscriptionEntity {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private String clientHandle;
    private String topic;
    private int qos;
    private boolean enableNotification;

    public SubscriptionEntity(String clientHandle, String topic, int qos, boolean enableNotification) {
        this.clientHandle = clientHandle;
        this.topic = topic;
        this.qos = qos;
        this.enableNotification = enableNotification;
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getClientHandle() {
        return clientHandle;
    }

    public void setClientHandle(String clientHandle) {
        this.clientHandle = clientHandle;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public int getQos() {
        return qos;
    }

    public void setQos(int qos) {
        this.qos = qos;
    }

    public boolean isEnableNotification() {
        return enableNotification;
    }

    public void setEnableNotification(boolean enableNotification) {
        this.enableNotification = enableNotification;
    }

    @Override
    public String toString() {
        return "Subscription{" +
                " clientHandle='" + clientHandle + '\'' +
                ", id='" + id + '\'' +
                ", topic=" + topic + '\'' +
                ", qos=" + qos + '\'' +
                ", enableNotification='" + enableNotification + '\'' +
                '}';
    }
}
