package com.shiyinghan.mqttdemo.module.presenter;

import com.shiyinghan.mqttdemo.base.presenter.BasePresenter;
import com.shiyinghan.mqttdemo.common.MyApplication;
import com.shiyinghan.mqttdemo.module.contract.ConnectionPortalContract;
import com.shiyinghan.mqttdemo.mqtt.MqttDatabase;
import com.shiyinghan.mqttdemo.mqtt.dao.SubscriptionDao;
import com.shiyinghan.mqttdemo.mqtt.entity.SubscriptionEntity;
import com.shiyinghan.mqttdemo.reactivex.observer.BaseHandleObserver;

import java.util.List;

import io.reactivex.annotations.NonNull;

public class ConnectionPortalPresenter extends BasePresenter<ConnectionPortalContract.View>
        implements ConnectionPortalContract.Presenter {
    private SubscriptionDao mSubscriptionDao;

    public ConnectionPortalPresenter(ConnectionPortalContract.View view) {
        super(view);
        mSubscriptionDao = MqttDatabase.getInstance(MyApplication.getInstance()).getSubscriptionDao();
    }

    @Override
    public void getSubscriptionList(String clientHandle) {
        addSubscription(mSubscriptionDao.findSubscriptionWithClientHandle(clientHandle), new BaseHandleObserver<List<SubscriptionEntity>>() {
            @Override
            public void onNext(@NonNull List<SubscriptionEntity> list) {
                mView.getSubscriptionListSuccess(list);
            }

            @Override
            public void onError(Throwable e) {
                super.onError(e);
                mView.getSubscriptionListFail();
            }
        });
    }
}
