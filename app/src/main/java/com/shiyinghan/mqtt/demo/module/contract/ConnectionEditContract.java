package com.shiyinghan.mqtt.demo.module.contract;

import com.shiyinghan.mqtt.demo.mqtt.entity.ConnectionEntity;

public interface ConnectionEditContract {
    interface Presenter {
        void saveConnection(ConnectionEntity entity);
    }

    interface View {
        void saveConnectionSuccess();

        void saveConnectionFail();
    }
}
