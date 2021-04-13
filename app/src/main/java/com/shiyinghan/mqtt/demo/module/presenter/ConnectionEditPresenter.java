package com.shiyinghan.mqtt.demo.module.presenter;

import com.shiyinghan.mqtt.demo.base.presenter.BasePresenter;
import com.shiyinghan.mqtt.demo.common.MyApplication;
import com.shiyinghan.mqtt.demo.mqtt.MqttDatabase;
import com.shiyinghan.mqtt.demo.mqtt.entity.ConnectionEntity;
import com.shiyinghan.mqtt.demo.module.contract.ConnectionEditContract;

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
