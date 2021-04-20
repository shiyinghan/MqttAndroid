package com.shiyinghan.mqtt.demo.module.presenter;

import com.shiyinghan.mqtt.demo.base.presenter.BasePresenter;
import com.shiyinghan.mqtt.demo.common.MyApplication;
import com.shiyinghan.mqtt.demo.mqtt.MqttClientFactory;
import com.shiyinghan.mqtt.demo.mqtt.MqttDatabase;
import com.shiyinghan.mqtt.demo.mqtt.entity.ConnectionEntity;
import com.shiyinghan.mqtt.demo.module.contract.ConnectionListContract;
import com.shiyinghan.mqtt.demo.reactivex.observer.BaseHandleObserver;

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

//                MqttClientFactory.clearClient(MyApplication.getInstance(), connectionEntity);
            }

            @Override
            public void onError(@NonNull Throwable e) {
                mView.deleteConnectionFail();
            }
        });
    }
}
