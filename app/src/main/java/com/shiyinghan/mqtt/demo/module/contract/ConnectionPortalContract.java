package com.shiyinghan.mqtt.demo.module.contract;

import com.shiyinghan.mqtt.demo.mqtt.entity.SubscriptionEntity;

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
