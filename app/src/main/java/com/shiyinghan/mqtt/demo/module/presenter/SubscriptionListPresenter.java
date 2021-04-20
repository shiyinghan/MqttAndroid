package com.shiyinghan.mqtt.demo.module.presenter;

import com.shiyinghan.mqtt.demo.base.presenter.BasePresenter;
import com.shiyinghan.mqtt.demo.common.MyApplication;
import com.shiyinghan.mqtt.demo.module.contract.SubscriptionListContract;
import com.shiyinghan.mqtt.demo.mqtt.MqttDatabase;
import com.shiyinghan.mqtt.demo.mqtt.dao.SubscriptionDao;
import com.shiyinghan.mqtt.demo.mqtt.entity.SubscriptionEntity;
import com.shiyinghan.mqtt.demo.reactivex.observer.BaseHandleObserver;

import java.util.List;

import io.reactivex.annotations.NonNull;
import io.reactivex.observers.DisposableCompletableObserver;
import io.reactivex.observers.DisposableSingleObserver;

public class SubscriptionListPresenter extends BasePresenter<SubscriptionListContract.View>
        implements SubscriptionListContract.Presenter {

    private SubscriptionDao mSubscriptionDao;

    public SubscriptionListPresenter(SubscriptionListContract.View view) {
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
    public void saveSubscription(SubscriptionEntity entity) {
        addSubscription(mSubscriptionDao.findSubscriptionByClientHandleAndTopic(
                entity.getClientHandle(), entity.getTopic()), new DisposableSingleObserver<List<SubscriptionEntity>>() {
            @Override
            public void onSuccess(@NonNull List<SubscriptionEntity> list) {
                if (list.size() > 0) {
                    entity.setId(list.get(0).getId());
                }

                addSubscription(mSubscriptionDao.insertSubscription(entity), new DisposableCompletableObserver() {
                    @Override
                    public void onComplete() {
                        mView.saveSubscriptionSuccess(entity);
                    }

                    @Override
                    public void onError(@NonNull Throwable e) {
                        mView.saveSubscriptionFail();
                    }
                });
            }

            @Override
            public void onError(@NonNull Throwable e) {
                mView.saveSubscriptionFail();
            }
        });
    }

    @Override
    public void deleteSubscription(SubscriptionEntity entity) {
        addSubscription(mSubscriptionDao.deleteSubscription(entity), new DisposableCompletableObserver() {
            @Override
            public void onComplete() {
                mView.deleteSubscriptionSuccess(entity);
            }

            @Override
            public void onError(@NonNull Throwable e) {
                mView.deleteSubscriptionFail();
            }
        });
    }
}
