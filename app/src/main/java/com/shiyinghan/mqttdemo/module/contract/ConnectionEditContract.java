package com.shiyinghan.mqttdemo.module.contract;

import com.shiyinghan.mqttdemo.mqtt.entity.ConnectionEntity;

public interface ConnectionEditContract {
    interface Presenter {
        void saveConnection(ConnectionEntity entity);
    }

    interface View {
        void saveConnectionSuccess();

        void saveConnectionFail();
    }
}
