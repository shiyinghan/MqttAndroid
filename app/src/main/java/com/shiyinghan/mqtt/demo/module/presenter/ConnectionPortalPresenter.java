package com.shiyinghan.mqtt.demo.module.presenter;

import com.shiyinghan.mqtt.demo.base.presenter.BasePresenter;
import com.shiyinghan.mqtt.demo.common.MyApplication;
import com.shiyinghan.mqtt.demo.module.contract.ConnectionPortalContract;
import com.shiyinghan.mqtt.demo.mqtt.MqttDatabase;
import com.shiyinghan.mqtt.demo.mqtt.dao.SubscriptionDao;
import com.shiyinghan.mqtt.demo.mqtt.entity.SubscriptionEntity;
import com.shiyinghan.mqtt.demo.reactivex.observer.BaseHandleObserver;

import java.util.List;

import io.reactivex.annotations.NonNull;
import io.reactivex.observers.DisposableCompletableObserver;

public class ConnectionPortalPresenter extends BasePresenter<ConnectionPortalContract.View>
        implements ConnectionPortalContract.Presenter {
    private SubscriptionDao mSubscriptionDao;

    public ConnectionPortalPresenter(ConnectionPortalContract.View view) {
        super(view);
        mSubscriptionDao = MqttDatabase.getInstance(MyApplication.getInstance()).getSubscriptionDao();
    }

    @Override
    public void getSubscriptionList(String clientHandle) {
        addSubscription(mSubscriptionDao.findSubscriptionByClientHandle(clientHandle), new BaseHandleObserver<List<SubscriptionEntity>>() {
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

    @Override
    public void deleteSubscriptionList(String clientHandle) {
        addSubscription(mSubscriptionDao.deleteSubscriptionByClientHandle(clientHandle), new DisposableCompletableObserver() {
            @Override
            public void onComplete() {
                mView.deleteSubscriptionListSuccess();
            }

            @Override
            public void onError(@NonNull Throwable e) {
                mView.deleteSubscriptionListFail();
            }
        });
    }
}
