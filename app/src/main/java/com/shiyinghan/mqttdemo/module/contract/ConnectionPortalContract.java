package com.shiyinghan.mqttdemo.module.contract;

import com.shiyinghan.mqttdemo.mqtt.entity.SubscriptionEntity;

import java.util.List;

public interface ConnectionPortalContract {
    interface Presenter {
        void getSubscriptionList(String clientHandle);
    }

    interface View {
        void getSubscriptionListSuccess(List<SubscriptionEntity> list);

        void getSubscriptionListFail();
    }
}
