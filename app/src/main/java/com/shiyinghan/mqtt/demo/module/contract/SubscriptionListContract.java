package com.shiyinghan.mqtt.demo.module.contract;

import com.shiyinghan.mqtt.demo.mqtt.entity.SubscriptionEntity;

import java.util.List;

public interface SubscriptionListContract {
    interface Presenter {
        void getSubscriptionListObservable(String clientHandle);

        void saveSubscription(SubscriptionEntity entity);

        void deleteSubscription(SubscriptionEntity entity);
    }

    interface View {
        void getSubscriptionListObservableSuccess(List<SubscriptionEntity> list);

        void getSubscriptionListObservableFail();

        void saveSubscriptionSuccess(SubscriptionEntity entity);

        void saveSubscriptionFail();

        void deleteSubscriptionSuccess(SubscriptionEntity entity);

        void deleteSubscriptionFail();
    }
}
