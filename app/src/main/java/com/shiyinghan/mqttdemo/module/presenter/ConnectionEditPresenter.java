package com.shiyinghan.mqttdemo.module.presenter;

import com.shiyinghan.mqttdemo.base.presenter.BasePresenter;
import com.shiyinghan.mqttdemo.common.MyApplication;
import com.shiyinghan.mqttdemo.mqtt.MqttDatabase;
import com.shiyinghan.mqttdemo.mqtt.entity.ConnectionEntity;
import com.shiyinghan.mqttdemo.module.contract.ConnectionEditContract;

import io.reactivex.annotations.NonNull;
import io.reactivex.observers.DisposableCompletableObserver;

public class ConnectionEditPresenter extends BasePresenter<ConnectionEditContract.View>
        implements ConnectionEditContract.Presenter {

    public ConnectionEditPresenter(ConnectionEditContract.View view) {
        super(view);
    }

    @Override
    public void saveConnection(ConnectionEntity entity) {
        addSubscription(MqttDatabase.getInstance(MyApplication.getInstance())
                .getConnectionDao().insertConnection(entity), new DisposableCompletableObserver() {
            @Override
            public void onComplete() {
                mView.saveConnectionSuccess();
            }

            @Override
            public void onError(@NonNull Throwable e) {
                mView.saveConnectionFail();
            }
        });
    }
}
