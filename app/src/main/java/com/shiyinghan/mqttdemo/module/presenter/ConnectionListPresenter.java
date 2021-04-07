package com.shiyinghan.mqttdemo.module.presenter;

import com.shiyinghan.mqttdemo.base.presenter.BasePresenter;
import com.shiyinghan.mqttdemo.common.MyApplication;
import com.shiyinghan.mqttdemo.mqtt.MqttDatabase;
import com.shiyinghan.mqttdemo.mqtt.entity.ConnectionEntity;
import com.shiyinghan.mqttdemo.module.contract.ConnectionListContract;
import com.shiyinghan.mqttdemo.reactivex.observer.BaseHandleObserver;

import java.util.List;

import io.reactivex.annotations.NonNull;
import io.reactivex.observers.DisposableCompletableObserver;

public class ConnectionListPresenter extends BasePresenter<ConnectionListContract.View>
        implements ConnectionListContract.Presenter {

    public ConnectionListPresenter(ConnectionListContract.View view) {
        super(view);
    }

    @Override
    public void getConnectionList() {
        addSubscription(MqttDatabase.getInstance(MyApplication.getInstance())
                .getConnectionDao().findConnectionAll(), new BaseHandleObserver<List<ConnectionEntity>>() {
            @Override
            public void onNext(@NonNull List<ConnectionEntity> connectionEntities) {
                mView.getConnectionListSuccess(connectionEntities);
            }

            @Override
            public void onError(@NonNull Throwable e) {
                mView.getConnectionListFail();
            }
        });
    }

    @Override
    public void deleteConnection(ConnectionEntity connectionEntity) {
        addSubscription(MqttDatabase.getInstance(MyApplication.getInstance())
                .getConnectionDao().deleteConnection(connectionEntity), new DisposableCompletableObserver() {
            @Override
            public void onComplete() {
                mView.deleteConnectionSuccess();
            }

            @Override
            public void onError(@NonNull Throwable e) {
                mView.deleteConnectionFail();
            }
        });
    }
}