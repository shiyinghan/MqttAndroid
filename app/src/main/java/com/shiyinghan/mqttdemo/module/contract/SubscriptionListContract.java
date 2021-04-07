package com.shiyinghan.mqttdemo.module.contract;

import com.shiyinghan.mqttdemo.mqtt.entity.SubscriptionEntity;

import java.util.List;

public interface SubscriptionListContract {
    interface Presenter {
        void getSubscriptionList(String clientHandle);

        void saveSubscription(SubscriptionEntity entity);

        void deleteSubscription(SubscriptionEntity entity);
    }

    interface View {
        void getSubscriptionListSuccess(List<SubscriptionEntity> list);

        void getSubscriptionListFail();

        void saveSubscriptionSuccess(SubscriptionEntity entity);

        void saveSubscriptionFail();

        void deleteSubscriptionSuccess(SubscriptionEntity entity);

        void deleteSubscriptionFail();
    }
}