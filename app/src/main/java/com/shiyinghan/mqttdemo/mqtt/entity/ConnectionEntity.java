package com.shiyinghan.mqttdemo.mqtt.entity;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

import java.io.Serializable;
import java.util.Objects;

@Entity
public class ConnectionEntity implements Serializable {

    @PrimaryKey(autoGenerate = true)
    private long id;

    private String clientId;

    private String server;

    private int port;

    private boolean cleanSession;

    private String username;

    private String password;

    private int timeout;

    private int keepAlive;

    /**
     * Topic for Last Will and Testament
     */
    private String lwtTopic;

    /**
     * Message for Last Will and Testament
     */
    private String lwtMessage;

    /**
     * Retain flag for Last Will and Testament
     */
    private boolean lwtRetain;

    /**
     * Qos for Last Will and Testament
     */
    private int lwtQos;

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getServer() {
        return server;
    }

    public void setServer(String server) {
        this.server = server;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public boolean isCleanSession() {
        return cleanSession;
    }

    public void setCleanSession(boolean cleanSession) {
        this.cleanSession = cleanSession;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getTimeout() {
        return timeout;
    }

    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }

    public int getKeepAlive() {
        return keepAlive;
    }

    public void setKeepAlive(int keepAlive) {
        this.keepAlive = keepAlive;
    }

    public String getLwtTopic() {
        return lwtTopic;
    }

    public void setLwtTopic(String lwtTopic) {
        this.lwtTopic = lwtTopic;
    }

    public String getLwtMessage() {
        return lwtMessage;
    }

    public void setLwtMessage(String lwtMessage) {
        this.lwtMessage = lwtMessage;
    }

    public boolean isLwtRetain() {
        return lwtRetain;
    }

    public void setLwtRetain(boolean lwtRetain) {
        this.lwtRetain = lwtRetain;
    }

    public int getLwtQos() {
        return lwtQos;
    }

    public void setLwtQos(int lwtQos) {
        this.lwtQos = lwtQos;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ConnectionEntity that = (ConnectionEntity) o;
        return id == that.id &&
                port == that.port &&
                cleanSession == that.cleanSession &&
                timeout == that.timeout &&
                keepAlive == that.keepAlive &&
                lwtRetain == that.lwtRetain &&
                lwtQos == that.lwtQos &&
                clientId.equals(that.clientId) &&
                server.equals(that.server) &&
                Objects.equals(username, that.username) &&
                Objects.equals(password, that.password) &&
                Objects.equals(lwtTopic, that.lwtTopic) &&
                Objects.equals(lwtMessage, that.lwtMessage);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, clientId, server, port, cleanSession, username, password, timeout, keepAlive, lwtTopic, lwtMessage, lwtRetain, lwtQos);
    }
}
