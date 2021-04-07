package com.shiyinghan.mqtt.android.service;

import android.os.Handler;
import android.os.Looper;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.internal.wire.MqttWireMessage;

/**
 * <p>
 * Implementation of the IMqttToken interface for use from within the
 * MqttAndroidClient implementation
 */
class MqttTokenAndroid implements IMqttToken {

    private IMqttActionListener listener;

    private volatile boolean isComplete;

    private volatile MqttException lastException;

    private final Object waitObject = new Object();

    private final MqttAndroidClient client;

    private Object userContext;

    private final String[] topics;

    /**
     * specifically for getMessageId
     */
    private IMqttToken delegate;

    private MqttException pendingException;

    private Handler handler = new Handler(Looper.getMainLooper());

    /**
     * Standard constructor
     *
     * @param client      used to pass MqttAndroidClient object
     * @param userContext used to pass context
     * @param listener    optional listener that will be notified when the action completes. Use null if not required.
     */
    MqttTokenAndroid(MqttAndroidClient client,
                     Object userContext, IMqttActionListener listener) {
        this(client, userContext, listener, null);
    }

    /**
     * Constructor for use with subscribe operations
     *
     * @param client      used to pass MqttAndroidClient object
     * @param userContext used to pass context
     * @param listener    optional listener that will be notified when the action completes. Use null if not required.
     * @param topics      topics to subscribe to, which can include wildcards.
     */
    MqttTokenAndroid(MqttAndroidClient client,
                     Object userContext, IMqttActionListener listener, String[] topics) {
        this.client = client;
        this.userContext = userContext;
        this.listener = listener;
        this.topics = topics;
    }

    /**
     * @see org.eclipse.paho.client.mqttv3.IMqttToken#waitForCompletion()
     */
    @Override
    public void waitForCompletion() throws MqttException, MqttSecurityException {
        synchronized (waitObject) {
            try {
                waitObject.wait();
            } catch (InterruptedException e) {
                // do nothing
            }
        }
        if (pendingException != null) {
            throw pendingException;
        }
    }

    /**
     * @see org.eclipse.paho.client.mqttv3.IMqttToken#waitForCompletion(long)
     */
    @Override
    public void waitForCompletion(long timeout) throws MqttException, MqttSecurityException {
        synchronized (waitObject) {
            try {
                waitObject.wait(timeout);
            } catch (InterruptedException e) {
                // do nothing
            }
            if (!isComplete) {
                throw new MqttException(MqttException.REASON_CODE_CLIENT_TIMEOUT);
            }
            if (pendingException != null) {
                throw pendingException;
            }
        }
    }

    /**
     * notify successful completion of the operation
     */
    void notifyComplete() {
        synchronized (waitObject) {
            isComplete = true;
            waitObject.notifyAll();
            handler.post(() -> {
                if (listener != null) {
                    listener.onSuccess(MqttTokenAndroid.this);
                }
            });
        }
    }

    /**
     * notify unsuccessful completion of the operation
     */
    void notifyFailure(Throwable exception) {
        synchronized (waitObject) {
            isComplete = true;
            if (exception instanceof MqttException) {
                pendingException = (MqttException) exception;
            } else {
                pendingException = new MqttException(exception);
            }
            waitObject.notifyAll();
            if (exception instanceof MqttException) {
                lastException = (MqttException) exception;
            }
            handler.post(() -> {
                if (listener != null) {
                    listener.onFailure(MqttTokenAndroid.this, exception);
                }
            });
        }

    }

    /**
     * @see org.eclipse.paho.client.mqttv3.IMqttToken#isComplete()
     */
    @Override
    public boolean isComplete() {
        return isComplete;
    }

    void setComplete(boolean complete) {
        isComplete = complete;
    }

    /**
     * @see org.eclipse.paho.client.mqttv3.IMqttToken#getException()
     */
    @Override
    public MqttException getException() {
        return lastException;
    }

    void setException(MqttException exception) {
        lastException = exception;
    }

    /**
     * @see org.eclipse.paho.client.mqttv3.IMqttToken#getClient()
     */
    @Override
    public IMqttAsyncClient getClient() {
        return client;
    }

    /**
     * @see org.eclipse.paho.client.mqttv3.IMqttToken#setActionCallback(IMqttActionListener)
     */
    @Override
    public void setActionCallback(IMqttActionListener listener) {
        this.listener = listener;
    }

    /**
     * @see org.eclipse.paho.client.mqttv3.IMqttToken#getActionCallback()
     */
    @Override
    public IMqttActionListener getActionCallback() {
        return listener;
    }

    /**
     * @see org.eclipse.paho.client.mqttv3.IMqttToken#getTopics()
     */
    @Override
    public String[] getTopics() {
        return topics;
    }

    /**
     * @see org.eclipse.paho.client.mqttv3.IMqttToken#setUserContext(Object)
     */
    @Override
    public void setUserContext(Object userContext) {
        this.userContext = userContext;
    }

    /**
     * @see org.eclipse.paho.client.mqttv3.IMqttToken#getUserContext()
     */
    @Override
    public Object getUserContext() {
        return userContext;
    }

    void setDelegate(IMqttToken delegate) {
        this.delegate = delegate;
    }

    /**
     * @see org.eclipse.paho.client.mqttv3.IMqttToken#getMessageId()
     */
    @Override
    public int getMessageId() {
        return (delegate != null) ? delegate.getMessageId() : 0;
    }

    @Override
    public MqttWireMessage getResponse() {
        return delegate.getResponse();
    }

    @Override
    public boolean getSessionPresent() {
        return delegate.getSessionPresent();
    }

    @Override
    public int[] getGrantedQos() {
        return delegate.getGrantedQos();
    }

}
