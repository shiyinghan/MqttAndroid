package com.shiyinghan.mqtt.demo.mqtt;

import android.content.Context;
import android.content.Intent;
import android.text.TextUtils;
import android.util.Log;

import com.shiyinghan.mqtt.demo.R;
import com.shiyinghan.mqtt.demo.common.Constant;
import com.shiyinghan.mqtt.demo.event.ConnectionLostEvent;
import com.shiyinghan.mqtt.demo.event.MessageArrivedEvent;
import com.shiyinghan.mqtt.demo.module.activity.ConnectionPortalActivity;
import com.shiyinghan.mqtt.demo.mqtt.dao.SubscriptionDao;
import com.shiyinghan.mqtt.demo.mqtt.entity.ConnectionEntity;

import com.shiyinghan.mqtt.android.service.MqttAndroidClient;
import com.shiyinghan.mqtt.demo.mqtt.entity.ReceivedMessageEntity;
import com.shiyinghan.mqtt.demo.mqtt.entity.SubscriptionEntity;
import com.shiyinghan.mqtt.demo.reactivex.RxUtils;
import com.shiyinghan.mqtt.demo.utils.NotifyUtil;

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.MqttCallback;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import org.greenrobot.eventbus.EventBus;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.reactivex.annotations.NonNull;
import io.reactivex.observers.DisposableCompletableObserver;
import io.reactivex.observers.DisposableSingleObserver;

public class MqttClientFactory {

    private static final Map<ConnectionEntity, MqttAndroidClient> sClientMap = new ConcurrentHashMap<>();

    public static MqttAndroidClient getClient(Context context, ConnectionEntity connectionEntity) {
        return getClient(context, connectionEntity, false);
    }

    public static MqttAndroidClient getClient(Context context, ConnectionEntity connectionEntity, boolean forceNewInstance) {
        MqttAndroidClient client = sClientMap.get(connectionEntity);
        if (client == null || forceNewInstance) {
            String uri = "tcp://" + connectionEntity.getServer() + ":" + connectionEntity.getPort();
            client = new MqttAndroidClient(context, uri, connectionEntity.getClientId());
            client.setCallback(new MyMqttCallBack(context, connectionEntity, client.getClientHandle()));
            sClientMap.put(connectionEntity, client);
        }
        return client;
    }

    public static void clearClient(Context context, ConnectionEntity connectionEntity) {
        MqttAndroidClient client = getClient(context, connectionEntity);
        RxUtils.addSubscription(MqttDatabase.getInstance(context).getSubscriptionDao().deleteSubscriptionByClientHandle(client.getClientHandle()), new DisposableCompletableObserver() {
            @Override
            public void onComplete() {

            }

            @Override
            public void onError(@NonNull Throwable e) {

            }
        });
        sClientMap.remove(connectionEntity);
    }

    public static MqttConnectOptions getConnectOptions(ConnectionEntity entity) {
        MqttConnectOptions options = new MqttConnectOptions();
        //设置清除回话session 可收到服务器之前发出的推送消息
        options.setCleanSession(entity.isCleanSession());
        //设置连接超时时间
        options.setConnectionTimeout(entity.getTimeout());
        //设置心跳包默认的发送间隔
        options.setKeepAliveInterval(entity.getKeepAlive());
        if (!TextUtils.isEmpty(entity.getUsername())) {
            //设置用户名（可选）
            options.setUserName(entity.getUsername());
        }
        if (!TextUtils.isEmpty(entity.getPassword())) {
            //设置密码（可选）
            options.setPassword(entity.getPassword().toCharArray());
        }
        if (!TextUtils.isEmpty(entity.getLwtTopic()) && !TextUtils.isEmpty(entity.getLwtMessage())) {
            //设置遗愿
            options.setWill(entity.getLwtTopic(), entity.getLwtMessage().getBytes(), entity.getLwtQos(), entity.isLwtRetain());
        }
        return options;
    }

    static class MyMqttCallBack implements MqttCallback {
        private static final String TAG = MyMqttCallBack.class.getSimpleName();

        private final Context mContext;
        private final ConnectionEntity mConnection;
        private final String mClientHandle;

        private final SubscriptionDao mSubscriptionDao;

        public MyMqttCallBack(Context context, ConnectionEntity connectionEntity, String clientHandle) {
            this.mContext = context;
            this.mConnection = connectionEntity;
            this.mClientHandle = clientHandle;
            this.mSubscriptionDao = MqttDatabase.getInstance(context).getSubscriptionDao();
        }

        @Override
        public void connectionLost(Throwable cause) {
            Log.d(TAG, "connectionLost : " + (cause != null ? cause.getLocalizedMessage() : "null"));
            EventBus.getDefault().post(new ConnectionLostEvent());

            //build intent
            Intent intent = new Intent(mContext, ConnectionPortalActivity.class);
            intent.putExtra(Constant.DATA, mConnection);

            //format string args
            Object[] notifyArgs = new String[2];
            notifyArgs[0] = mConnection.getClientId();
            notifyArgs[1] = mConnection.getServer();

            //notify the user
            NotifyUtil.showNotification(mContext, mContext.getString(R.string.notify_connection_lost_content, notifyArgs), intent, R.string.notify_connection_lost_title);
        }

        @Override
        public void messageArrived(String topic, MqttMessage message) throws Exception {
            Log.d(TAG, "messageArrived : " + topic);
            ReceivedMessageEntity receivedMessage = new ReceivedMessageEntity(mClientHandle, topic, message);
            EventBus.getDefault().postSticky(new MessageArrivedEvent(receivedMessage));

            RxUtils.addSubscription(mSubscriptionDao.findSubscriptionByClientHandleAndTopic(mClientHandle, topic), new DisposableSingleObserver<List<SubscriptionEntity>>() {
                @Override
                public void onSuccess(@NonNull List<SubscriptionEntity> list) {
                    if (list.size() == 0) {
                        return;
                    }
                    if (list.get(0).isEnableNotification()) {

                        //build intent
                        Intent intent = new Intent(mContext, ConnectionPortalActivity.class);
                        intent.putExtra(Constant.DATA, mConnection);

                        //format string args
                        Object[] notifyArgs = new String[3];
                        notifyArgs[0] = mConnection.getClientId();
                        notifyArgs[1] = new String(message.getPayload());
                        notifyArgs[2] = topic;

                        //notify the user
                        NotifyUtil.showNotification(mContext, mContext.getString(R.string.notify_message_content, notifyArgs), intent, R.string.notify_message_title);
                    }
                }

                @Override
                public void onError(@NonNull Throwable e) {

                }
            });
        }

        @Override
        public void deliveryComplete(IMqttDeliveryToken token) {
            Log.d(TAG, "deliveryComplete : " + token);
        }
    }
}
