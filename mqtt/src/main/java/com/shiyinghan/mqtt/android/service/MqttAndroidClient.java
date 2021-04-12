package com.shiyinghan.mqtt.android.service;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.PowerManager;

import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttAsyncClient;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttMessageListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttClientPersistence;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.eclipse.paho.client.mqttv3.MqttPersistenceException;
import org.eclipse.paho.client.mqttv3.MqttSecurityException;
import org.eclipse.paho.client.mqttv3.MqttToken;
import org.eclipse.paho.client.mqttv3.persist.MqttDefaultFilePersistence;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;

/**
 * Enables an android application to communicate with an MQTT server using non-blocking methods.
 * <p>
 * Using the MQTT
 * android service to actually interface with MQTT server. It provides android applications a simple programming interface to all features of the MQTT version 3.1
 * specification including:
 * </p>
 * <ul>
 * <li>connect
 * <li>publish
 * <li>subscribe
 * <li>unsubscribe
 * <li>disconnect
 * </ul>
 */
public class MqttAndroidClient implements IMqttAsyncClient {
    private static final String TAG = MqttAndroidClient.class.getSimpleName();

    private static final String SERVICE_NAME = "com.shiyinghan.mqtt.android.service.MqttService";

    private Context myContext;

    /**
     * The Android Service which will keep our mqtt client
     */
    private static MqttService mqttService;

    /**
     * An identifier for the underlying client connection, which we can pass to the service
     */
    private String clientHandle;

    // Connection data
    private final String serverURI;
    private final String clientId;
    private MqttClientPersistence persistence = null;
    private MqttConnectOptions connectOptions = new MqttConnectOptions();
    private MqttTokenAndroid connectToken;

    // The MqttCallback provided by the application
    private MqttCallback callback;

    private MqttTraceHandler traceCallback;
    private boolean traceEnabled = true;

    // our client object - instantiated on connect
    private MqttAsyncClient myClient = null;

    private AlarmPingSender alarmPingSender = null;

    private boolean needReconnect = false;

    /**
     * Saved MqttTokenAndroid of published messages ,
     * so we can handle "deliveryComplete" callbacks from the mqttClient
     */
    private Map<IMqttActionListener, MqttTokenAndroid> tokenAndroidMap = new ConcurrentHashMap<>();

    private PowerManager.WakeLock wakelock = null;
    private String wakeLockTag = null;

    /**
     * ServiceConnection to process when we bind to our service
     */
    private final ServiceConnection serviceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName name, IBinder binder) {
            mqttService = ((MqttService.MqttServiceBinder) binder).getService();
            // now that we have the service available, we can actually
            // connect...
            doConnect();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            mqttService = null;
        }
    };

    /**
     * Constructor - create an MqttAndroidClient to communicate with MQTT server
     *
     * @param context   our "parent" context - we make callbacks to it
     * @param serverURI the URI of the MQTT server to which we will connect
     * @param clientId  the name by which we will identify ourselves to the MQTT server
     */
    public MqttAndroidClient(Context context, String serverURI, String clientId) {
        this(context, serverURI, clientId, null, new MqttTraceCallback());
    }

    /**
     * Constructor - create an MqttAndroidClient to communicate with MQTT server
     *
     * @param context       our "parent" context - we make callbacks to it
     * @param serverURI     the URI of the MQTT server to which we will connect
     * @param clientId      the name by which we will identify ourselves to the MQTT server
     * @param traceCallback the callback to trace runtime information
     */
    public MqttAndroidClient(Context context, String serverURI, String clientId, MqttTraceHandler traceCallback) {
        this(context, serverURI, clientId, null, traceCallback);
    }

    /**
     * Constructor - create an MqttAndroidClient to communicate with MQTT server
     *
     * @param context     our "parent" context - we make callbacks to it
     * @param serverURI   the URI of the MQTT server to which we will connect
     * @param clientId    the name by which we will identify ourselves to the MQTT server
     * @param persistence the persistence class to use to store in-flight message. If
     *                    null then the default persistence mechanism is used
     */
    public MqttAndroidClient(Context context, String serverURI, String clientId, MqttClientPersistence persistence) {
        this(context, serverURI, clientId, persistence, new MqttTraceCallback());
    }

    /**
     * Constructor - create an MqttAndroidClient to communicate with MQTT server
     *
     * @param context       our "parent" context - we make callbacks to it
     * @param serverURI     the URI of the MQTT server to which we will connect
     * @param clientId      the name by which we will identify ourselves to the MQTT server
     * @param persistence   the persistence class to use to store in-flight message. If
     *                      null then the default persistence mechanism is used
     * @param traceCallback the callback to trace runtime information
     */
    public MqttAndroidClient(Context context, String serverURI, String clientId, MqttClientPersistence persistence, MqttTraceHandler traceCallback) {
        this.myContext = context;
        this.serverURI = serverURI;
        this.clientId = clientId;
        this.persistence = persistence;
        this.traceCallback = traceCallback;

        this.clientHandle = serverURI + ":" + clientId + ":" + myContext.getApplicationInfo().packageName;

        StringBuilder stringBuilder = new StringBuilder(this.getClass().getCanonicalName());
        stringBuilder.append(" ");
        stringBuilder.append(clientId);
        stringBuilder.append(" ");
        stringBuilder.append("on host ");
        stringBuilder.append(serverURI);
        wakeLockTag = stringBuilder.toString();
    }

    /**
     * Connects to an MQTT server using the default options.
     * <p>
     * The default options are specified in {@link MqttConnectOptions} class.
     * </p>
     *
     * @return token used to track and wait for the connect to complete. The
     * token will be passed to the callback methods if a callback is
     * set.
     * @throws MqttException for any connected problems
     * @see #connect(MqttConnectOptions, Object, IMqttActionListener)
     */
    @Override
    public IMqttToken connect() throws MqttException, MqttSecurityException {
        return connect(myContext, null);
    }

    /**
     * Connects to an MQTT server using the provided connect options.
     * <p>
     * The connection will be established using the options specified in the
     * {@link MqttConnectOptions} parameter.
     * </p>
     *
     * @param options a set of connection parameters that override the defaults.
     * @return token used to track and wait for the connect to complete. The
     * token will be passed to any callback that has been set.
     * @throws MqttException for any connected problems
     * @see #connect(MqttConnectOptions, Object, IMqttActionListener)
     */
    @Override
    public IMqttToken connect(MqttConnectOptions options) throws MqttException, MqttSecurityException {
        return connect(options, myContext, null);
    }

    /**
     * Connects to an MQTT server using the default options.
     * <p>
     * The default options are specified in {@link MqttConnectOptions} class.
     * </p>
     *
     * @param userContext optional object used to pass context to the callback. Use null
     *                    if not required.
     * @param callback    optional listener that will be notified when the connect
     *                    completes. Use null if not required.
     * @return token used to track and wait for the connect to complete. The
     * token will be passed to any callback that has been set.
     * @throws MqttException for any connected problems
     * @see #connect(MqttConnectOptions, Object, IMqttActionListener)
     */
    @Override
    public IMqttToken connect(Object userContext, IMqttActionListener callback) throws MqttException, MqttSecurityException {
        return connect(new MqttConnectOptions(), userContext, callback);
    }

    /**
     * Connects to an MQTT server using the specified options.
     * <p>
     * The server to connect to is specified on the constructor. It is
     * recommended to call {@link #setCallback(MqttCallback)} prior to
     * connecting in order that messages destined for the client can be accepted
     * as soon as the client is connected.
     * </p>
     *
     * <p>
     * The method returns control before the connect completes. Completion can
     * be tracked by:
     * </p>
     * <ul>
     * <li>Waiting on the returned token {@link IMqttToken#waitForCompletion()}
     * or</li>
     * <li>Passing in a callback {@link IMqttActionListener}</li>
     * </ul>
     *
     * @param options     a set of connection parameters that override the defaults.
     * @param userContext optional object for used to pass context to the callback. Use
     *                    null if not required.
     * @param callback    optional listener that will be notified when the connect
     *                    completes. Use null if not required.
     * @return token used to track and wait for the connect to complete. The
     * token will be passed to any callback that has been set.
     * @throws MqttException for any connected problems, including communication errors
     */
    @Override
    public IMqttToken connect(MqttConnectOptions options, Object userContext, IMqttActionListener callback) throws MqttException, MqttSecurityException {

        MqttTokenAndroid token = new MqttTokenAndroid(this, userContext, callback);

        if (options == null) {
            connectOptions = new MqttConnectOptions();
        } else {
            connectOptions = options;
        }
        connectToken = token;

        /*
         * The actual connection depends on the service, which we start and bind
         * to here, but which we can't actually use until the serviceConnection
         * onServiceConnected() method has run (asynchronously), so the
         * connection itself takes place in the onServiceConnected() method
         */
        if (mqttService == null) { // First time - must bind to the service
            Intent serviceStartIntent = new Intent();
            serviceStartIntent.setClassName(myContext, SERVICE_NAME);
            Object service = myContext.startService(serviceStartIntent);
            if (service == null) {
                IMqttActionListener listener = connectToken.getActionCallback();
                if (listener != null) {
                    listener.onFailure(connectToken, new RuntimeException(
                            "cannot start service " + SERVICE_NAME));
                }
            }

            // We bind with BIND_SERVICE_FLAG (0), leaving us the manage the lifecycle
            // until the last time it is stopped by a call to stopService()
            myContext.bindService(serviceStartIntent, serviceConnection,
                    Context.BIND_AUTO_CREATE);
        } else {
            doConnect();
        }

        return token;
    }

    /**
     * Actually do the mqtt connect operation
     */
    private void doConnect() {
        traceDebug(TAG, "Connecting {" + serverURI + "} as {" + clientId + "}");

        needReconnect = true;

        MqttService.putClient(clientHandle, this);

        // if it's a clean session,
        if (connectOptions.isCleanSession()) {
            // discard old data
            mqttService.getMessageStore().clearArrivedMessages(clientHandle);
        }

        try {
            if (persistence == null) {
                initMqttClientPersistence();
            }

//            long testTimeStamp = System.currentTimeMillis();

            IMqttActionListener listener = new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    traceDebug(TAG, "Connect success!");
//                    Log.d(TAG, "cost time:" + (System.currentTimeMillis() - testTimeStamp));
                    doAfterConnectSuccess();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable throwable) {
                    traceException(TAG, "Connect fail, call connect to reconnect.reason:" + throwable.getMessage(), new Exception(throwable));
                    doAfterConnectFail(throwable);
                }
            };

            // if myClient is null, then create a new connection
            if (myClient == null) {
                if (alarmPingSender == null) {
                    alarmPingSender = new AlarmPingSender(mqttService);
                }
                myClient = new MqttAsyncClient(serverURI, clientId, persistence, alarmPingSender);
                myClient.setCallback(new MqttCallbackExtendedAndroid());
            }

            traceDebug(TAG, "Do Real connect!");
            myClient.connect(connectOptions, myContext, listener);
        } catch (MqttException e) {
            if (e.getReasonCode() == MqttException.REASON_CODE_CLIENT_CONNECTED) {
                traceDebug(TAG, "Connect return: " + e.getMessage());
                doAfterConnectSuccess();
            } else if (e.getReasonCode() == MqttException.REASON_CODE_CONNECT_IN_PROGRESS
                    || e.getReasonCode() == MqttException.REASON_CODE_CLIENT_DISCONNECTING
                    || e.getReasonCode() == MqttException.REASON_CODE_CLIENT_CLOSED) {
                traceDebug(TAG, "Connect return: " + e.getMessage());
            } else {
                traceException(TAG, "Exception occurred attempting to connect: " + e.getMessage(), e);
                doAfterConnectFail(e);
            }
        }
    }

    private void initMqttClientPersistence() {
        // ask Android where we can put files
        File myDir = myContext.getExternalFilesDir(TAG);

        if (myDir == null) {
            // No external storage, use internal storage instead.
            myDir = myContext.getDir(TAG, Context.MODE_PRIVATE);

            if (myDir == null) {
                //Shouldn't happen.
                String msg = "Error! No external and internal storage available";
                traceError(TAG, msg);
                connectToken.notifyFailure(new MqttPersistenceException());
                return;
            }
        }

        // use that to setup MQTT client persistence storage
        persistence = new MqttDefaultFilePersistence(myDir.getAbsolutePath());
    }

    private void doAfterConnectSuccess() {
        //since the device's cpu can go to sleep, acquire a wakelock and drop it later.
        acquireWakeLock();
        connectToken.notifyComplete();
        deliverBacklog();
        releaseWakeLock();
    }

    private void doAfterConnectFail(Throwable e) {
        //since the device's cpu can go to sleep, acquire a wakelock and drop it later.
        acquireWakeLock();
        connectToken.notifyFailure(e);
        releaseWakeLock();
    }

    /**
     * Disconnects from the server.
     * <p>
     * An attempt is made to quiesce the client allowing outstanding work to
     * complete before disconnecting. It will wait for a maximum of 30 seconds
     * for work to quiesce before disconnecting. This method must not be called
     * from inside {@link MqttCallback} methods.
     * </p>
     *
     * @return token used to track and wait for disconnect to complete. The
     * token will be passed to any callback that has been set.
     * @throws MqttException for problems encountered while disconnecting
     * @see #disconnect(long, Object, IMqttActionListener)
     */
    @Override
    public IMqttToken disconnect() throws MqttException {
        return this.disconnect(myContext, null);
    }

    /**
     * Disconnects from the server.
     * <p>
     * An attempt is made to quiesce the client allowing outstanding work to
     * complete before disconnecting. It will wait for a maximum of the
     * specified quiesce time for work to complete before disconnecting. This
     * method must not be called from inside {@link MqttCallback} methods.
     * </p>
     *
     * @param quiesceTimeout the amount of time in milliseconds to allow for existing work
     *                       to finish before disconnecting. A value of zero or less means
     *                       the client will not quiesce.
     * @return token used to track and wait for disconnect to complete. The
     * token will be passed to the callback methods if a callback is
     * set.
     * @throws MqttException for problems encountered while disconnecting
     * @see #disconnect(long, Object, IMqttActionListener)
     */
    @Override
    public IMqttToken disconnect(long quiesceTimeout) throws MqttException {
        return this.disconnect(quiesceTimeout, myContext, null);
    }

    /**
     * Disconnects from the server.
     * <p>
     * An attempt is made to quiesce the client allowing outstanding work to
     * complete before disconnecting. It will wait for a maximum of 30 seconds
     * for work to quiesce before disconnecting. This method must not be called
     * from inside {@link MqttCallback} methods.
     * </p>
     *
     * @param userContext optional object used to pass context to the callback. Use null
     *                    if not required.
     * @param callback    optional listener that will be notified when the disconnect
     *                    completes. Use null if not required.
     * @return token used to track and wait for the disconnect to complete. The
     * token will be passed to any callback that has been set.
     * @throws MqttException for problems encountered while disconnecting
     * @see #disconnect(long, Object, IMqttActionListener)
     */
    @Override
    public IMqttToken disconnect(Object userContext, IMqttActionListener callback) throws MqttException {
        traceDebug(TAG, "Disconnecting");
        MqttTokenAndroid token = new MqttTokenAndroid(this, userContext, callback);

        needReconnect = false;

        if ((myClient != null) && (myClient.isConnected())) {
            IMqttActionListener listener = new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    traceDebug(TAG, "Disconnect success!");
                    doAfterDisconnectSuccess(token);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable throwable) {
                    traceException(TAG, "Disconnect fail, call connect to reconnect.reason:" + throwable.getMessage(), new Exception(throwable));
                    doAfterDisconnectFail(token, throwable);
                }
            };
            try {
                myClient.disconnect(userContext, listener);
            } catch (Exception e) {
                traceException(TAG, "Disconnect fail :" + e.getMessage(), e);
                token.notifyFailure(e);
            }
        } else {
            String msg = "Disconnect success: Client is not connected";
            traceError(TAG, msg);
            doAfterDisconnectSuccess(token);
        }

        return token;
    }

    /**
     * Disconnects from the server.
     * <p>
     * The client will wait for {@link MqttCallback} methods to complete. It
     * will then wait for up to the quiesce timeout to allow for work which has
     * already been initiated to complete. For instance when a QoS 2 message has
     * started flowing to the server but the QoS 2 flow has not completed.It
     * prevents new messages being accepted and does not send any messages that
     * have been accepted but not yet started delivery across the network to the
     * server. When work has completed or after the quiesce timeout, the client
     * will disconnect from the server. If the cleanSession flag was set to
     * false and next time it is also set to false in the connection, the
     * messages made in QoS 1 or 2 which were not previously delivered will be
     * delivered this time.
     * </p>
     * <p>
     * This method must not be called from inside {@link MqttCallback} methods.
     * </p>
     * <p>
     * The method returns control before the disconnect completes. Completion
     * can be tracked by:
     * </p>
     * <ul>
     * <li>Waiting on the returned token {@link IMqttToken#waitForCompletion()}
     * or</li>
     * <li>Passing in a callback {@link IMqttActionListener}</li>
     * </ul>
     *
     * @param quiesceTimeout the amount of time in milliseconds to allow for existing work
     *                       to finish before disconnecting. A value of zero or less means
     *                       the client will not quiesce.
     * @param userContext    optional object used to pass context to the callback. Use null
     *                       if not required.
     * @param callback       optional listener that will be notified when the disconnect
     *                       completes. Use null if not required.
     * @return token used to track and wait for the disconnect to complete. The
     * token will be passed to any callback that has been set.
     * @throws MqttException for problems encountered while disconnecting
     */
    @Override
    public IMqttToken disconnect(long quiesceTimeout, Object userContext, IMqttActionListener callback) throws MqttException {
        traceDebug(TAG, "Disconnecting");
        MqttTokenAndroid token = new MqttTokenAndroid(this, userContext, callback);

        needReconnect = false;

        if ((myClient != null) && (myClient.isConnected())) {
            IMqttActionListener listener = new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    traceDebug(TAG, "Disconnect success!");
                    doAfterDisconnectSuccess(token);
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable throwable) {
                    traceException(TAG, "Disconnect fail:" + throwable.getMessage(), new Exception(throwable));
                    doAfterDisconnectFail(token, throwable);
                }
            };
            try {
                myClient.disconnect(quiesceTimeout, userContext, listener);
            } catch (Exception e) {
                traceException(TAG, "Disconnect fail :" + e.getMessage(), e);
                token.notifyFailure(e);
            }
        } else {
            String msg = "Disconnect success: Client is not connected";
            traceError(TAG, msg);
            doAfterDisconnectSuccess(token);
        }

        return token;
    }

    /**
     * Process a notification that we have disconnected
     *
     * @param token
     */
    private void doAfterDisconnectSuccess(MqttTokenAndroid token) {
        if (token != null) {
            token.notifyComplete();
        }
        if (callback != null) {
            callback.connectionLost(null);
        }
    }

    /**
     * Process a notification that we have disconnected
     *
     * @param token
     */
    private void doAfterDisconnectFail(MqttTokenAndroid token, Throwable e) {
        if (token != null) {
            token.notifyFailure(e);
        }
        if (callback != null) {
            callback.connectionLost(e);
        }
    }

    @Override
    public void disconnectForcibly() throws MqttException {
        if (myClient != null) {
            myClient.disconnectForcibly();
        }
    }

    @Override
    public void disconnectForcibly(long disconnectTimeout) throws MqttException {
        if (myClient != null) {
            myClient.disconnectForcibly(disconnectTimeout);
        }
    }

    @Override
    public void disconnectForcibly(long quiesceTimeout, long disconnectTimeout) throws MqttException {
        if (myClient != null) {
            myClient.disconnectForcibly(quiesceTimeout, disconnectTimeout);
        }
    }

    /**
     * Determines if this client is currently connected to the server.
     *
     * @return <code>true</code> if connected, <code>false</code> otherwise.
     */
    @Override
    public boolean isConnected() {
        return myClient != null && myClient.isConnected();
    }

    /**
     * Returns the client ID used by this client.
     * <p>
     * All clients connected to the same server or server farm must have a
     * unique ID.
     * </p>
     *
     * @return the client ID used by this client.
     */
    @Override
    public String getClientId() {
        return clientId;
    }

    /**
     * Returns the URI address of the server used by this client.
     * <p>
     * The format of the returned String is the same as that used on the
     * constructor.
     * </p>
     *
     * @return the server's address, as a URI String.
     */
    @Override
    public String getServerURI() {
        return serverURI;
    }

    /**
     * Publishes a message to a topic on the server.
     * <p>
     * A convenience method, which will create a new {@link MqttMessage} object
     * with a byte array payload and the specified QoS, and then publish it.
     * </p>
     *
     * @param topic    to deliver the message to, for example "finance/stock/ibm".
     * @param payload  the byte array to use as the payload
     * @param qos      the Quality of Service to deliver the message at. Valid values
     *                 are 0, 1 or 2.
     * @param retained whether or not this message should be retained by the server.
     * @return token used to track and wait for the publish to complete. The
     * token will be passed to any callback that has been set.
     * @throws MqttPersistenceException when a problem occurs storing the message
     * @throws IllegalArgumentException if value of QoS is not 0, 1 or 2.
     * @throws MqttException            for other errors encountered while publishing the message.
     *                                  For instance, too many messages are being processed.
     * @see #publish(String, MqttMessage, Object, IMqttActionListener)
     */
    @Override
    public IMqttDeliveryToken publish(String topic, byte[] payload, int qos, boolean retained)
            throws MqttException, MqttPersistenceException {
        return publish(topic, payload, qos, retained, myContext, null);
    }

    /**
     * Publishes a message to a topic on the server.
     * <p>
     * A convenience method, which will create a new {@link MqttMessage} object
     * with a byte array payload, the specified QoS and retained, then publish it.
     * </p>
     *
     * @param topic       to deliver the message to, for example "finance/stock/ibm".
     * @param payload     the byte array to use as the payload
     * @param qos         the Quality of Service to deliver the message at. Valid values
     *                    are 0, 1 or 2.
     * @param retained    whether or not this message should be retained by the server.
     * @param userContext optional object used to pass context to the callback. Use null
     *                    if not required.
     * @param callback    optional listener that will be notified when message delivery
     *                    has completed to the requested quality of service
     * @return token used to track and wait for the publish to complete. The
     * token will be passed to any callback that has been set.
     * @throws MqttPersistenceException when a problem occurs storing the message
     * @throws IllegalArgumentException if value of QoS is not 0, 1 or 2.
     * @throws MqttException            for other errors encountered while publishing the message.
     *                                  For instance client not connected.
     * @see #publish(String, MqttMessage, Object, IMqttActionListener)
     */
    @Override
    public IMqttDeliveryToken publish(String topic, byte[] payload, int qos, boolean retained, Object userContext, IMqttActionListener callback)
            throws MqttException, MqttPersistenceException {
        String traceMsgSuffix = " : ({ topic: " + topic + "},{ payload: " + Arrays.toString(payload) + "},{ qos: " + qos + "},{ userContext: " + userContext + "}";
        traceDebug(TAG, "Publishing" + traceMsgSuffix);

        MqttMessage message = new MqttMessage(payload);
        message.setQos(qos);
        message.setRetained(retained);
        MqttTokenAndroid token = new MqttTokenAndroid(this, userContext, callback);

        IMqttDeliveryToken sendToken = null;

        if (myClient != null) {
            IMqttActionListener listener = new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    traceDebug(TAG, "Publish success" + traceMsgSuffix);
                    token.notifyComplete();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    traceDebug(TAG, "Publish failed" + traceMsgSuffix);
                    token.notifyFailure(exception);
                }
            };

            try {
                setTokenAndroidMap(listener, token);
                sendToken = myClient.publish(topic, payload, qos, retained,
                        userContext, listener);
            } catch (Exception e) {
                traceException(TAG, e.getMessage(), e);
                token.notifyFailure(e);
            }
        } else {
            String msg = "Publish action error: Client is null, so not sending message";
            traceError(TAG, msg);
            token.notifyFailure(new Exception(msg));
        }

        token.setDelegate(sendToken);
        return sendToken;
    }

    /**
     * Publishes a message to a topic on the server. Takes an
     * {@link MqttMessage} message and delivers it to the server at the
     * requested quality of service.
     *
     * @param topic   to deliver the message to, for example "finance/stock/ibm".
     * @param message to deliver to the server
     * @return token used to track and wait for the publish to complete. The
     * token will be passed to any callback that has been set.
     * @throws MqttPersistenceException when a problem occurs storing the message
     * @throws IllegalArgumentException if value of QoS is not 0, 1 or 2.
     * @throws MqttException            for other errors encountered while publishing the message.
     *                                  For instance client not connected.
     * @see #publish(String, MqttMessage, Object, IMqttActionListener)
     */
    @Override
    public IMqttDeliveryToken publish(String topic, MqttMessage message) throws MqttException, MqttPersistenceException {
        return publish(topic, message, myContext, null);
    }

    /**
     * Publishes a message to a topic on the server.
     * <p>
     * Once this method has returned cleanly, the message has been accepted for
     * publication by the client and will be delivered on a background thread.
     * In the event the connection fails or the client stops, Messages will be
     * delivered to the requested quality of service once the connection is
     * re-established to the server on condition that:
     * </p>
     * <ul>
     * <li>The connection is re-established with the same clientID
     * <li>The original connection was made with (@link
     * MqttConnectOptions#setCleanSession(boolean)} set to false
     * <li>The connection is re-established with (@link
     * MqttConnectOptions#setCleanSession(boolean)} set to false
     * <li>Depending when the failure occurs QoS 0 messages may not be
     * delivered.
     * </ul>
     *
     * <p>
     * When building an application, the design of the topic tree should take
     * into account the following principles of topic name syntax and semantics:
     * </p>
     *
     * <ul>
     * <li>A topic must be at least one character long.</li>
     * <li>Topic names are case sensitive. For example, <em>ACCOUNTS</em> and
     * <em>Accounts</em> are two different topics.</li>
     * <li>Topic names can include the space character. For example,
     * <em>Accounts
     * 	payable</em> is a valid topic.</li>
     * <li>A leading "/" creates a distinct topic. For example,
     * <em>/finance</em> is different from <em>finance</em>. <em>/finance</em>
     * matches "+/+" and "/+", but not "+".</li>
     * <li>Do not include the null character (Unicode <em>\x0000</em>) in any topic.</li>
     * </ul>
     *
     * <p>
     * The following principles apply to the construction and content of a topic
     * tree:
     * </p>
     *
     * <ul>
     * <li>The length is limited to 64k but within that there are no limits to
     * the number of levels in a topic tree.</li>
     * <li>There can be any number of root nodes; that is, there can be any
     * number of topic trees.</li>
     * </ul>
     * <p>
     * The method returns control before the publish completes. Completion can
     * be tracked by:
     * </p>
     * <ul>
     * <li>Setting an {@link IMqttAsyncClient#setCallback(MqttCallback)} where
     * the {@link MqttCallback#deliveryComplete(IMqttDeliveryToken)} method will
     * be called.</li>
     * <li>Waiting on the returned token {@link MqttToken#waitForCompletion()}
     * or</li>
     * <li>Passing in a callback {@link IMqttActionListener} to this method</li>
     * </ul>
     *
     * @param topic       to deliver the message to, for example "finance/stock/ibm".
     * @param message     to deliver to the server
     * @param userContext optional object used to pass context to the callback. Use null
     *                    if not required.
     * @param callback    optional listener that will be notified when message delivery
     *                    has completed to the requested quality of service
     * @return token used to track and wait for the publish to complete. The
     * token will be passed to callback methods if set.
     * @throws MqttPersistenceException when a problem occurs storing the message
     * @throws IllegalArgumentException if value of QoS is not 0, 1 or 2.
     * @throws MqttException            for other errors encountered while publishing the message.
     *                                  For instance, client not connected.
     * @see MqttMessage
     */
    @Override
    public IMqttDeliveryToken publish(String topic, MqttMessage message, Object userContext, IMqttActionListener callback) throws MqttException, MqttPersistenceException {
        String traceMsgSuffix = " : ({ topic: " + topic + "},{ message: " + message + "},{ userContext: " + userContext + "}";
        traceDebug(TAG, "Publishing" + traceMsgSuffix);

        MqttTokenAndroid token = new MqttTokenAndroid(this, userContext, callback);

        IMqttDeliveryToken sendToken = null;

        if (myClient != null) {
            IMqttActionListener listener = new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    traceDebug(TAG, "Publish success" + traceMsgSuffix);
                    token.notifyComplete();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    traceDebug(TAG, "Publish failed" + traceMsgSuffix);
                    token.notifyFailure(exception);
                }
            };
            try {
                setTokenAndroidMap(listener, token);
                sendToken = myClient.publish(topic, message, userContext, listener);
            } catch (Exception e) {
                traceException(TAG, e.getMessage(), e);
                token.notifyFailure(e);
            }
        } else {
            String msg = "Publish action error: Client is null, so not sending message";
            traceError(TAG, msg);
            token.notifyFailure(new Exception(msg));
        }
        return sendToken;
    }

    /**
     * Subscribe to a topic, which may include wildcards.
     *
     * @param topicFilter the topic to subscribe to, which can include wildcards.
     * @param qos         the maximum quality of service at which to subscribe. Messages
     *                    published at a lower quality of service will be received at
     *                    the published QoS. Messages published at a higher quality of
     *                    service will be received using the QoS specified on the
     *                    subscription.
     * @return token used to track and wait for the subscribe to complete. The
     * token will be passed to callback methods if set.
     * @throws MqttSecurityException for security related problems
     * @throws MqttException         for non security related problems
     * @see #subscribe(String[], int[], Object, IMqttActionListener)
     */
    @Override
    public IMqttToken subscribe(String topicFilter, int qos) throws MqttException {
        return subscribe(topicFilter, qos, myContext, null);
    }

    /**
     * Subscribe to a topic, which may include wildcards.
     *
     * @param topicFilter the topic to subscribe to, which can include wildcards.
     * @param qos         the maximum quality of service at which to subscribe. Messages
     *                    published at a lower quality of service will be received at
     *                    the published QoS. Messages published at a higher quality of
     *                    service will be received using the QoS specified on the
     *                    subscription.
     * @param userContext optional object used to pass context to the callback. Use null
     *                    if not required.
     * @param callback    optional listener that will be notified when subscribe has
     *                    completed
     * @return token used to track and wait for the subscribe to complete. The
     * token will be passed to callback methods if set.
     * @throws MqttException if there was an error when registering the subscription.
     * @see #subscribe(String[], int[], Object, IMqttActionListener)
     */
    @Override
    public IMqttToken subscribe(String topicFilter, int qos, Object userContext, IMqttActionListener callback) throws MqttException {
        String traceMsgSuffix = " : ({ topicFilter: " + topicFilter + "},{ qos: " + qos + "},{ userContext: " + userContext + "}";
        traceDebug(TAG, "Subscribing" + traceMsgSuffix);

        MqttTokenAndroid token = new MqttTokenAndroid(this, userContext,
                callback, new String[]{topicFilter});

        if (myClient != null) {
            IMqttActionListener listener = new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    traceDebug(TAG, "Subscribe success" + traceMsgSuffix);
                    token.notifyComplete();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    traceDebug(TAG, "Subscribe failed" + traceMsgSuffix);
                    token.notifyFailure(exception);
                }
            };
            try {
                myClient.subscribe(topicFilter, qos, userContext, listener);
            } catch (Exception e) {
                traceException(TAG, e.getMessage(), e);
                token.notifyFailure(e);
            }
        } else {
            String msg = "Subscribe action error: Client is null";
            traceError(TAG, msg);
            token.notifyFailure(new Exception(msg));
        }

        return token;
    }

    /**
     * Subscribe to multiple topics, each topic may include wildcards.
     *
     * <p>
     * Provides an optimized way to subscribe to multiple topics compared to
     * subscribing to each one individually.
     * </p>
     *
     * @param topicFilters one or more topics to subscribe to, which can include
     *                     wildcards
     * @param qos          the maximum quality of service at which to subscribe. Messages
     *                     published at a lower quality of service will be received at
     *                     the published QoS. Messages published at a higher quality of
     *                     service will be received using the QoS specified on the
     *                     subscription.
     * @return token used to track and wait for the subscription to complete. The
     * token will be passed to callback methods if set.
     * @throws MqttSecurityException for security related problems
     * @throws MqttException         for non security related problems
     * @see #subscribe(String[], int[], Object, IMqttActionListener)
     */
    @Override
    public IMqttToken subscribe(String[] topicFilters, int[] qos) throws MqttException {
        return subscribe(topicFilters, qos, myContext, null);
    }

    /**
     * Subscribes to multiple topics, each topic may include wildcards.
     * <p>
     * Provides an optimized way to subscribe to multiple topics compared to
     * subscribing to each one individually.
     * </p>
     * <p>
     * The {@link #setCallback(MqttCallback)} method should be called before
     * this method, otherwise any received messages will be discarded.
     * </p>
     * <p>
     * If (@link MqttConnectOptions#setCleanSession(boolean)} was set to true,
     * when connecting to the server, the subscription remains in place until
     * either:
     * </p>
     * <ul>
     * <li>The client disconnects</li>
     * <li>An unsubscribe method is called to unsubscribe the topic</li>
     * </ul>
     * <p>
     * If (@link MqttConnectOptions#setCleanSession(boolean)} was set to false,
     * when connecting to the server, the subscription remains in place
     * until either:
     * </p>
     * <ul>
     * <li>An unsubscribe method is called to unsubscribe the topic</li>
     * <li>The next time the client connects with cleanSession set to true
     * </ul>
     * <p>With cleanSession set to false the MQTT server will store messages
     * on behalf of the client when the client is not connected. The next time
     * the client connects with the <b>same client ID</b> the server will
     * deliver the stored messages to the client.
     * </p>
     *
     * <p>
     * The "topic filter" string is used when subscription may contain special
     * characters, which allows you to subscribe to multiple topics at once.
     * <dl>
     * <dt>Topic level separator</dt>
     * <dd>The forward slash (/) is used to separate each level within a topic
     * tree and provide a hierarchical structure to the topic space. The use of
     * the topic level separator is significant when the two wildcard characters
     * are encountered in topics specified by subscribers.</dd>
     *
     * <dt>Multi-level wildcard</dt>
     * <dd>
     * <p>
     * The number sign (#) is a wildcard character that matches any number of
     * levels within a topic. For example, if you subscribe to <span><span
     * class="filepath">finance/stock/ibm/#</span></span>, you receive messages
     * on these topics:
     * </p>
     * <ul>
     *     <li><pre>finance/stock/ibm</pre></li>
     *     <li><pre>finance/stock/ibm/closingprice</pre></li>
     *     <li><pre>finance/stock/ibm/currentprice</pre></li>
     * </ul>
     *
     * <p>
     * The multi-level wildcard can represent zero or more levels. Therefore,
     * <em>finance/#</em> can also match the singular <em>finance</em>, where
     * <em>#</em> represents zero levels. The topic level separator is
     * meaningless in this context, because there are no levels to separate.
     * </p>
     *
     * <p>
     * The <span>multi-level</span> wildcard can be specified only on its own or
     * next to the topic level separator character. Therefore, <em>#</em> and
     * <em>finance/#</em> are both valid, but <em>finance#</em> is not valid.
     * <span>The multi-level wildcard must be the last character used within the
     * topic tree. For example, <em>finance/#</em> is valid but
     * <em>finance/#/closingprice</em> is not valid.</span>
     * </p>
     * </dd>
     *
     * <dt>Single-level wildcard</dt>
     * <dd>
     * <p>
     * The plus sign (+) is a wildcard character that matches only one topic
     * level. For example, <em>finance/stock/+</em> matches
     * <em>finance/stock/ibm</em> and <em>finance/stock/xyz</em>, but not
     * <em>finance/stock/ibm/closingprice</em>. Also, because the single-level
     * wildcard matches only a single level, <em>finance/+</em> does not match
     * <em>finance</em>.
     * </p>
     *
     * <p>
     * Use the single-level wildcard at any level in the topic tree, and in
     * conjunction with the multilevel wildcard. Specify the single-level
     * wildcard next to the topic level separator, except when it is specified
     * on its own. Therefore, <em>+</em> and <em>finance/+</em> are both valid,
     * but <em>finance+</em> is not valid. <span>The single-level wildcard can
     * be used at the end of the topic tree or within the topic tree. For
     * example, <em>finance/+</em> and <em>finance/+/ibm</em> are both
     * valid.</span>
     * </p>
     * </dd>
     * </dl>
     * <p>
     * The method returns control before the subscribe completes. Completion can
     * be tracked by:
     * </p>
     * <ul>
     * <li>Waiting on the supplied token {@link MqttToken#waitForCompletion()}
     * or</li>
     * <li>Passing in a callback {@link IMqttActionListener} to this method</li>
     * </ul>
     *
     * @param topicFilters one or more topics to subscribe to, which can include
     *                     wildcards
     * @param qos          the maximum quality of service to subscribe each topic
     *                     at.Messages published at a lower quality of service will be
     *                     received at the published QoS. Messages published at a higher
     *                     quality of service will be received using the QoS specified on
     *                     the subscription.
     * @param userContext  optional object used to pass context to the callback. Use null
     *                     if not required.
     * @param callback     optional listener that will be notified when subscribe has
     *                     completed
     * @return token used to track and wait for the subscribe to complete. The
     * token will be passed to callback methods if set.
     * @throws MqttException            if there was an error registering the subscription.
     * @throws IllegalArgumentException if the two supplied arrays are not the same size.
     */
    @Override
    public IMqttToken subscribe(String[] topicFilters, int[] qos, Object userContext, IMqttActionListener callback) throws MqttException {
        String traceMsgSuffix = " :  ({ topicFilters: " + Arrays.toString(topicFilters) + "},{ qos: " + Arrays.toString(qos) + "},{ userContext : " + userContext + "}";
        traceDebug(TAG, "Subscribing" + traceMsgSuffix);

        MqttTokenAndroid token = new MqttTokenAndroid(this, userContext, callback, topicFilters);

        if (myClient != null) {
            IMqttActionListener listener = new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    traceDebug(TAG, "Subscribe success" + traceMsgSuffix);
                    token.notifyComplete();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    traceDebug(TAG, "Subscribe failed" + traceMsgSuffix);
                    token.notifyFailure(exception);
                }
            };

            try {
                myClient.subscribe(topicFilters, qos, userContext, listener);
            } catch (Exception e) {
                traceException(TAG, e.getMessage(), e);
                token.notifyFailure(e);
            }
        } else {
            String msg = "Subscribe action error: Client is null";
            traceError(TAG, msg);
            token.notifyFailure(new Exception(msg));
        }

        return token;
    }

    /**
     * Subscribe to a topic, which may include wildcards.
     *
     * @param topicFilter     the topic to subscribe to, which can include wildcards.
     * @param qos             the maximum quality of service at which to subscribe. Messages
     *                        published at a lower quality of service will be received at the published
     *                        QoS.  Messages published at a higher quality of service will be received using
     *                        the QoS specified on the subscribe.
     * @param userContext     optional object used to pass context to the callback. Use
     *                        null if not required.
     * @param callback        optional listener that will be notified when subscribe
     *                        has completed
     * @param messageListener a callback to handle incoming messages
     * @return token used to track and wait for the subscribe to complete. The token
     * will be passed to callback methods if set.
     * @throws MqttException if there was an error registering the subscription.
     * @see #subscribe(String[], int[], Object, IMqttActionListener)
     */
    @Override
    public IMqttToken subscribe(String topicFilter, int qos, Object userContext, IMqttActionListener callback, IMqttMessageListener messageListener)
            throws MqttException {
        return subscribe(new String[]{topicFilter}, new int[]{qos}, userContext, callback, new IMqttMessageListener[]{messageListener});
    }

    /**
     * Subscribe to a topic, which may include wildcards.
     *
     * @param topicFilter     the topic to subscribe to, which can include wildcards.
     * @param qos             the maximum quality of service at which to subscribe. Messages
     *                        published at a lower quality of service will be received at the published
     *                        QoS.  Messages published at a higher quality of service will be received using
     *                        the QoS specified on the subscribe.
     * @param messageListener a callback to handle incoming messages
     * @return token used to track and wait for the subscribe to complete. The token
     * will be passed to callback methods if set.
     * @throws MqttException if there was an error registering the subscription.
     * @see #subscribe(String[], int[], Object, IMqttActionListener)
     */
    @Override
    public IMqttToken subscribe(String topicFilter, int qos, IMqttMessageListener messageListener)
            throws MqttException {
        return subscribe(topicFilter, qos, myContext, null, messageListener);
    }

    /**
     * Subscribe to multiple topics, each of which may include wildcards.
     *
     * <p>Provides an optimized way to subscribe to multiple topics compared to
     * subscribing to each one individually.</p>
     *
     * @param topicFilters     one or more topics to subscribe to, which can include wildcards
     * @param qos              the maximum quality of service at which to subscribe. Messages
     *                         published at a lower quality of service will be received at the published
     *                         QoS.  Messages published at a higher quality of service will be received using
     *                         the QoS specified on the subscribe.
     * @param messageListeners an array of callbacks to handle incoming messages
     * @return token used to track and wait for the subscribe to complete. The token
     * will be passed to callback methods if set.
     * @throws MqttException if there was an error registering the subscription.
     * @see #subscribe(String[], int[], Object, IMqttActionListener)
     */
    @Override
    public IMqttToken subscribe(String[] topicFilters, int[] qos, IMqttMessageListener[] messageListeners)
            throws MqttException {
        return subscribe(topicFilters, qos, myContext, null, messageListeners);
    }

    /**
     * Subscribe to multiple topics, each of which may include wildcards.
     *
     * <p>Provides an optimized way to subscribe to multiple topics compared to
     * subscribing to each one individually.</p>
     *
     * @param topicFilters     one or more topics to subscribe to, which can include wildcards
     * @param qos              the maximum quality of service at which to subscribe. Messages
     *                         published at a lower quality of service will be received at the published
     *                         QoS.  Messages published at a higher quality of service will be received using
     *                         the QoS specified on the subscribe.
     * @param userContext      optional object used to pass context to the callback. Use
     *                         null if not required.
     * @param callback         optional listener that will be notified when subscribe
     *                         has completed
     * @param messageListeners an array of callbacks to handle incoming messages
     * @return token used to track and wait for the subscribe to complete. The token
     * will be passed to callback methods if set.
     * @throws MqttException if there was an error registering the subscription.
     * @see #subscribe(String[], int[], Object, IMqttActionListener)
     */
    @Override
    public IMqttToken subscribe(String[] topicFilters, int[] qos, Object userContext, IMqttActionListener callback, IMqttMessageListener[] messageListeners)
            throws MqttException {
        String traceMsgSuffix = " :  ({ topicFilters: " + Arrays.toString(topicFilters) + "},{ qos: " + Arrays.toString(qos) + "},{ userContext: " + userContext + "}";
        traceDebug(TAG, "Subscribing" + traceMsgSuffix);

        MqttTokenAndroid token = new MqttTokenAndroid(this, userContext, callback, topicFilters);

        if (myClient != null) {
            IMqttActionListener listener = new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    traceDebug(TAG, "Subscribe success" + traceMsgSuffix);
                    token.notifyComplete();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    traceDebug(TAG, "Subscribe failed" + traceMsgSuffix);
                    token.notifyFailure(exception);
                }
            };
            try {
                myClient.subscribe(topicFilters, qos, userContext, listener, messageListeners);
            } catch (Exception e) {
                traceException(TAG, e.getMessage(), e);
                token.notifyFailure(e);
            }
        } else {
            String msg = "Subscribe action error: Client is null";
            traceError(TAG, msg);
            token.notifyFailure(new Exception(msg));
        }

        return token;
    }

    /**
     * Requests the server unsubscribe the client from a topic.
     *
     * @param topicFilter the topic to unsubscribe from. It must match a topic specified
     *                    on an earlier subscribe.
     * @return token used to track and wait for the unsubscribe to complete. The
     * token will be passed to callback methods if set.
     * @throws MqttException if there was an error unregistering the subscription.
     * @see #unsubscribe(String[], Object, IMqttActionListener)
     */
    @Override
    public IMqttToken unsubscribe(String topicFilter) throws MqttException {
        return unsubscribe(topicFilter, myContext, null);
    }

    /**
     * Requests the server to unsubscribe the client from one or more topics.
     *
     * @param topicFilters one or more topics to unsubscribe from. Each topic must match
     *                     one specified on an earlier subscription.
     * @return token used to track and wait for the unsubscribe to complete. The
     * token will be passed to callback methods if set.
     * @throws MqttException if there was an error unregistering the subscription.
     * @see #unsubscribe(String[], Object, IMqttActionListener)
     */
    @Override
    public IMqttToken unsubscribe(String[] topicFilters) throws MqttException {
        return unsubscribe(topicFilters, myContext, null);
    }

    /**
     * Requests the server to unsubscribe the client from a topics.
     *
     * @param topicFilter the topic to unsubscribe from. It must match a topic specified
     *                    on an earlier subscribe.
     * @param userContext optional object used to pass context to the callback. Use null
     *                    if not required.
     * @param callback    optional listener that will be notified when unsubscribe has
     *                    completed
     * @return token used to track and wait for the unsubscribe to complete. The
     * token will be passed to callback methods if set.
     * @throws MqttException if there was an error unregistering the subscription.
     * @see #unsubscribe(String[], Object, IMqttActionListener)
     */
    @Override
    public IMqttToken unsubscribe(String topicFilter, Object userContext, IMqttActionListener callback)
            throws MqttException {
        String traceMsgSuffix = " : ({ topicFilter: " + topicFilter + "},{ userContext: " + userContext + "})";
        traceDebug(TAG, "Unsubscribing" + traceMsgSuffix);

        MqttTokenAndroid token = new MqttTokenAndroid(this, userContext, callback);

        if ((myClient != null) && (myClient.isConnected())) {
            IMqttActionListener listener = new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    traceDebug(TAG, "Unsubscrib success" + traceMsgSuffix);
                    token.notifyComplete();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    traceDebug(TAG, "Unsubscrib failed" + traceMsgSuffix);
                    token.notifyFailure(exception);
                }
            };

            try {
                myClient.unsubscribe(topicFilter, userContext, listener);
            } catch (Exception e) {
                traceException(TAG, e.getMessage(), e);
                token.notifyFailure(e);
            }
        } else {
            String msg = "Unsubscribe action error: Client is null";
            traceError(TAG, msg);
            token.notifyFailure(new Exception(msg));
        }

        return token;
    }

    /**
     * Requests the server to unsubscribe the client from one or more topics.
     * <p>
     * Unsubcribing is the opposite of subscribing. When the server receives the
     * unsubscribe request it looks to see if it can find a matching
     * subscription for the client and then removes it. After this point the
     * server will send no more messages to the client for this subscription.
     * </p>
     * <p>
     * The topic(s) specified on the unsubscribe must match the topic(s)
     * specified in the original subscribe request for the unsubscribe to
     * succeed
     * </p>
     * <p>
     * The method returns control before the unsubscribe completes. Completion
     * can be tracked by:
     * </p>
     * <ul>
     * <li>Waiting on the returned token {@link MqttToken#waitForCompletion()}
     * or</li>
     * <li>Passing in a callback {@link IMqttActionListener} to this method</li>
     * </ul>
     *
     * @param topicFilters one or more topics to unsubscribe from. Each topic must match
     *                     one specified on an earlier subscription.
     * @param userContext  optional object used to pass context to the callback. Use null
     *                     if not required.
     * @param callback     optional listener that will be notified when unsubscribe has
     *                     completed
     * @return token used to track and wait for the unsubscribe to complete. The
     * token will be passed to callback methods if set.
     * @throws MqttException if there was an error unregistering the subscription.
     */
    @Override
    public IMqttToken unsubscribe(String[] topicFilters, Object userContext, IMqttActionListener callback)
            throws MqttException {
        String traceMsgSuffix = " : ({ topicFilter: " + Arrays.toString(topicFilters) + "},{ userContext: " + userContext + "})";
        traceDebug(TAG, "Unsubscribing" + traceMsgSuffix);

        MqttTokenAndroid token = new MqttTokenAndroid(this, userContext, callback);

        if (myClient != null) {
            IMqttActionListener listener = new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    traceDebug(TAG, "Unsubscrib success" + traceMsgSuffix);
                    token.notifyComplete();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                    traceDebug(TAG, "Unsubscrib failed" + traceMsgSuffix);
                    token.notifyFailure(exception);
                }
            };
            try {
                myClient.unsubscribe(topicFilters, userContext, listener);
            } catch (Exception e) {
                traceException(TAG, e.getMessage(), e);
                token.notifyFailure(e);
            }
        } else {
            String msg = "Unsubscribe action error: Client is null";
            traceError(TAG, msg);
            token.notifyFailure(new Exception(msg));
        }

        return token;
    }

    @Override
    public boolean removeMessage(IMqttDeliveryToken token) throws MqttException {
        if (myClient != null) {
            return myClient.removeMessage(token);
        }
        return false;
    }

    /**
     * Sets a callback listener to use for events that happen asynchronously.
     * <p>
     * There are a number of events that the listener will be notified about.
     * These include:
     * </p>
     * <ul>
     * <li>A new message has arrived and is ready to be processed</li>
     * <li>The connection to the server has been lost</li>
     * <li>Delivery of a message to the server has completed</li>
     * </ul>
     * <p>
     * Other events that track the progress of an individual operation such as
     * connect and subscribe can be tracked using the {@link MqttToken} returned
     * from each non-blocking method or using setting a
     * {@link IMqttActionListener} on the non-blocking method.
     * <p>
     *
     * @param callback which will be invoked for certain asynchronous events
     * @see MqttCallback
     */
    @Override
    public void setCallback(MqttCallback callback) {
        this.callback = callback;
    }

    /**
     * Returns the delivery tokens for any outstanding publish operations.
     * <p>
     * If a client has been restarted and there are messages that were in the
     * process of being delivered when the client stopped, this method returns a
     * token for each in-flight message to enable the delivery to be tracked.
     * Alternately the {@link MqttCallback#deliveryComplete(IMqttDeliveryToken)}
     * callback can be used to track the delivery of outstanding messages.
     * </p>
     * <p>
     * If a client connects with cleanSession true then there will be no
     * delivery tokens as the cleanSession option deletes all earlier state. For
     * state to be remembered the client must connect with cleanSession set to
     * false
     * </P>
     *
     * @return zero or more delivery tokens
     */
    @Override
    public IMqttDeliveryToken[] getPendingDeliveryTokens() {
        if (myClient != null) {
            return myClient.getPendingDeliveryTokens();
        }
        return null;
    }

    @Override
    public void setManualAcks(boolean manualAcks) {
        throw new UnsupportedOperationException();
    }

    /**
     * Reconnect<br>
     * Only appropriate if cleanSession is false and we were connected.
     * Declare as synchronized to avoid multiple calls to this method to send connect
     * multiple times
     */
    @Override
    public void reconnect() throws MqttException {
        traceDebug(TAG, "Reconnecting");
        if (myClient != null) {
            try {
                myClient.reconnect();
            } catch (MqttException e) {
                if (e.getReasonCode() == MqttException.REASON_CODE_CLIENT_CONNECTED) {
                    traceDebug(TAG, "Reconnect return: " + e.getMessage());
                    doAfterConnectSuccess();
                } else if (e.getReasonCode() == MqttException.REASON_CODE_CONNECT_IN_PROGRESS
                        || e.getReasonCode() == MqttException.REASON_CODE_CLIENT_DISCONNECTING
                        || e.getReasonCode() == MqttException.REASON_CODE_CLIENT_CLOSED) {
                    traceDebug(TAG, "Reconnect return: " + e.getMessage());
                } else {
                    traceException(TAG, "Exception occurred attempting to reconnect: " + e.getMessage(), e);
                    doAfterConnectFail(e);
                }
            }
        }
    }

    @Override
    public void messageArrivedComplete(int messageId, int qos) throws MqttException {
        if (myClient != null) {
            myClient.messageArrivedComplete(messageId, qos);
        }
    }

    /**
     * Sets the DisconnectedBufferOptions for this client
     *
     * @param bufferOpts the DisconnectedBufferOptions
     */
    @Override
    public void setBufferOpts(DisconnectedBufferOptions bufferOpts) {
        if (myClient != null) {
            myClient.setBufferOpts(bufferOpts);
        }
    }

    @Override
    public int getBufferedMessageCount() {
        if (myClient == null) {
            return 0;
        }
        return myClient.getBufferedMessageCount();
    }

    @Override
    public MqttMessage getBufferedMessage(int bufferIndex) {
        if (myClient == null) {
            return null;
        }
        return myClient.getBufferedMessage(bufferIndex);
    }

    @Override
    public void deleteBufferedMessage(int bufferIndex) {
        if (myClient != null) {
            myClient.deleteBufferedMessage(bufferIndex);
        }
    }

    @Override
    public int getInFlightMessageCount() {
        if (myClient != null) {
            return myClient.getInFlightMessageCount();
        }
        return 0;
    }

    @Override
    public void close() throws MqttException {
        traceDebug(TAG, "close()");
        try {
            if (myClient != null) {
                myClient.close();
            }
        } catch (MqttException e) {
            traceException(TAG, e.getMessage(), e);
        }
    }

    public String getClientHandle() {
        return clientHandle;
    }

    synchronized void notifyOnline() {
        traceDebug(TAG, "Online reconnect with client:" + getClientId() + '/' + getServerURI());

        if (myClient == null) {
            traceError(TAG, "Online reconnect myClient = null. Will not do reconnect");
            return;
        }

        try {
            if (needReconnect && !connectOptions.isCleanSession()) {
                // use the activityToke the same with action connect
                traceDebug(TAG, "Online do real connect!");

                IMqttActionListener listener = new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken asyncActionToken) {
                        // since the device's cpu can go to sleep, acquire a
                        // wakelock and drop it later.
                        traceDebug(TAG, "Online Reconnect Success!");
                        traceDebug(TAG, "DeliverBacklog when online reconnected.");
                        doAfterConnectSuccess();
                    }

                    @Override
                    public void onFailure(IMqttToken asyncActionToken, Throwable exception) {
                        traceDebug(TAG, "Online reconnect failed!");
                        doAfterConnectFail(exception);
                    }
                };

                myClient.connect(connectOptions, myContext, listener);
            }
        } catch (MqttException e) {
            if (e.getReasonCode() == MqttException.REASON_CODE_CLIENT_CONNECTED) {
                traceDebug(TAG, "Online reconnect return: " + e.getMessage());
                doAfterConnectSuccess();
            } else if (e.getReasonCode() == MqttException.REASON_CODE_CONNECT_IN_PROGRESS
                    || e.getReasonCode() == MqttException.REASON_CODE_CLIENT_DISCONNECTING
                    || e.getReasonCode() == MqttException.REASON_CODE_CLIENT_CLOSED) {
                traceDebug(TAG, "Online reconnect return: " + e.getMessage());
            } else {
                traceException(TAG, "Exception occurred attempting to online reconnect: " + e.getMessage(), e);
                doAfterConnectFail(e);
            }
        }
    }

    /**
     * Receive notification that we are offline<br>
     * if cleanSession is true, we need to regard this as a disconnection
     */
    synchronized void notifyOffline() {
        traceDebug(TAG, "Offline with client:" + getClientId() + '/' + getServerURI());

        if (isConnected() && callback != null && !connectOptions.isCleanSession()) {
            Exception e = new Exception("Android offline");
            callback.connectionLost(e);
        }
    }

    /**
     * Attempt to deliver any outstanding messages we've received but which the
     * application hasn't acknowledged. If "cleanSession" was specified, we'll
     * have already purged any such messages from our messageStore.
     */
    private void deliverBacklog() {
        Iterator<MessageStore.StoredMessage> backlog = mqttService.getMessageStore()
                .getAllArrivedMessages(clientHandle);
        while (backlog.hasNext()) {
            MessageStore.StoredMessage msgArrived = backlog.next();
            messageArrivedAction(msgArrived.getMessageId(),
                    msgArrived.getTopic(), msgArrived.getMessage());
        }
    }

    /**
     * Process notification of a message's arrival
     *
     * @param messageId
     * @param topic
     * @param message
     */
    private void messageArrivedAction(String messageId, String topic, MqttMessage message) {
        if (callback != null) {

            try {
                callback.messageArrived(topic, message);
                mqttService.acknowledgeMessageArrival(clientHandle, messageId);

                // let the service discard the saved message details
            } catch (Exception e) {
                // Swallow the exception
            }
        }
    }

    /**
     * Store MqttTokenAndroid of published message so we can handle "deliveryComplete"
     * callbacks from the mqttClient
     *
     * @param listener
     * @param androidToken
     */
    private void setTokenAndroidMap(IMqttActionListener listener, MqttTokenAndroid androidToken) {
        tokenAndroidMap.put(listener, androidToken);
    }

    /**
     * Acquires a partial wake lock for this client
     */
    private void acquireWakeLock() {
        if (wakelock == null) {
            PowerManager pm = (PowerManager) myContext
                    .getSystemService(Service.POWER_SERVICE);
            wakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    wakeLockTag);
        }
        wakelock.acquire();
    }

    /**
     * Releases the currently held wake lock for this client
     */
    private void releaseWakeLock() {
        if (wakelock != null && wakelock.isHeld()) {
            wakelock.release();
        }
    }

    public void setAlarmPingSender(AlarmPingSender alarmPingSender) {
        this.alarmPingSender = alarmPingSender;
    }

    /**
     * identify the callback to be invoked when making tracing calls back into
     * the Activity
     *
     * @param traceCallback handler
     */
    public void setTraceCallback(MqttTraceHandler traceCallback) {
        this.traceCallback = traceCallback;
    }

    /**
     * turn tracing on and off
     *
     * @param traceEnabled set <code>true</code> to enable trace, otherwise, set
     *                     <code>false</code> to disable trace
     */
    public void setTraceEnabled(boolean traceEnabled) {
        this.traceEnabled = traceEnabled;
    }

    private void traceDebug(String tag, String message) {
        if (traceEnabled && traceCallback != null) {
            traceCallback.traceDebug(tag, message);
        }
    }

    private void traceError(String tag, String message) {
        if (traceEnabled && traceCallback != null) {
            traceCallback.traceError(tag, message);
        }
    }

    private void traceException(String tag, String message, Exception e) {
        if (traceEnabled && traceCallback != null) {
            traceCallback.traceException(tag, message, e);
        }
    }

    /**
     * Get the SSLSocketFactory using SSL key store and password
     * <p>
     * A convenience method, which will help user to create a SSLSocketFactory
     * object
     * </p>
     *
     * @param keyStore the SSL key store which is generated by some SSL key tool,
     *                 such as keytool in Java JDK
     * @param password the password of the key store which is set when the key store
     *                 is generated
     * @return SSLSocketFactory used to connect to the server with SSL
     * authentication
     * @throws MqttSecurityException if there was any error when getting the SSLSocketFactory
     */
    public static SSLSocketFactory getSSLSocketFactory(InputStream keyStore, String password) throws MqttSecurityException {
        try {
            SSLContext ctx = null;
            SSLSocketFactory sslSockFactory = null;
            KeyStore ts;
            ts = KeyStore.getInstance("BKS");
            ts.load(keyStore, password.toCharArray());
            TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
            tmf.init(ts);
            TrustManager[] tm = tmf.getTrustManagers();
            ctx = SSLContext.getInstance("TLSv1");
            ctx.init(null, tm, null);

            sslSockFactory = ctx.getSocketFactory();
            return sslSockFactory;

        } catch (KeyStoreException | CertificateException | IOException | NoSuchAlgorithmException | KeyManagementException e) {
            throw new MqttSecurityException(e);
        }
    }

    class MqttCallbackExtendedAndroid implements MqttCallbackExtended {

        @Override
        public void connectComplete(boolean reconnect, String serverURI) {
            // This is called differently from a normal connect

            if (callback instanceof MqttCallbackExtended) {
                ((MqttCallbackExtended) callback).connectComplete(reconnect, serverURI);
            }
        }

        /**
         * Callback for connectionLost
         *
         * @param cause the exeception causing the break in communications
         */
        @Override
        public void connectionLost(Throwable cause) {
            traceDebug(TAG, "connectionLost(" + cause.getMessage() + ")");
            try {
                if (myClient.isConnected()) {
                    if (!connectOptions.isAutomaticReconnect()) {
                        myClient.disconnect(null, new IMqttActionListener() {

                            @Override
                            public void onSuccess(IMqttToken asyncActionToken) {
                                // No action
                            }

                            @Override
                            public void onFailure(IMqttToken asyncActionToken,
                                                  Throwable exception) {
                                // No action
                            }
                        });
                    } else {
                        // Using the new Automatic reconnect functionality.
                        // We can't force a disconnection, but we can speed one up
                        alarmPingSender.schedule(100);
                    }
                }
            } catch (Exception e) {
                // ignore it - we've done our best
            }

            if (callback != null) {
                callback.connectionLost(cause);
            }

            // client has lost connection no need for wake lock
            releaseWakeLock();
        }

        /**
         * Callback when a message is received
         *
         * @param topic   the topic on which the message was received
         * @param message the message itself
         */
        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            traceDebug(TAG, "messageArrived(" + topic + ",{" + message.toString() + "})");

            if (callback != null) {
                String messageId = mqttService.getMessageStore().storeArrived(clientHandle,
                        topic, message);
                try {
                    callback.messageArrived(topic, message);
                    mqttService.acknowledgeMessageArrival(clientHandle, messageId);

                    // let the service discard the saved message details
                } catch (Exception e) {
                    // Swallow the exception
                }
            }
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
            traceDebug(TAG, "deliveryComplete(" + token + ")");

            if (!tokenAndroidMap.containsKey(token.getActionCallback()) || tokenAndroidMap.get(token.getActionCallback()) == null) {
                traceError(TAG, "savedSendDataMap data error");
            }
            MqttTokenAndroid androidToken = tokenAndroidMap.get(token.getActionCallback());

            // If I don't know about the MqttTokenAndroid, it's irrelevant
            if (androidToken != null) {

                androidToken.notifyComplete();

                if (callback != null) {
                    callback.deliveryComplete(token);
                }
            }

            // this notification will have kept the connection alive but send the previously sechudled ping anyway
        }
    }
}
