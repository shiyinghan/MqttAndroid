package com.shiyinghan.mqtt.demo.event;

import com.shiyinghan.mqtt.demo.mqtt.entity.ReceivedMessageEntity;

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
