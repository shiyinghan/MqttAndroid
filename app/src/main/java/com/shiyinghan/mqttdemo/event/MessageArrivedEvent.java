package com.shiyinghan.mqttdemo.event;

import com.shiyinghan.mqttdemo.mqtt.entity.ReceivedMessageEntity;

public class MessageArrivedEvent {
    private ReceivedMessageEntity receivedMessage;

    public MessageArrivedEvent(ReceivedMessageEntity receivedMessage) {
        this.receivedMessage = receivedMessage;
    }

    public ReceivedMessageEntity getReceivedMessage() {
        return receivedMessage;
    }

    public void setReceivedMessage(ReceivedMessageEntity receivedMessage) {
        this.receivedMessage = receivedMessage;
    }
}
