package com.shiyinghan.mqttdemo.module.contract;

import com.shiyinghan.mqttdemo.mqtt.entity.ConnectionEntity;

import java.util.List;

public interface ConnectionListContract {

    interface Presenter {
        void getConnectionList();

        void deleteConnection(ConnectionEntity connectionEntity);
    }

    interface View {
        void getConnectionListSuccess(List<ConnectionEntity> connections);

        void getConnectionListFail();

        void deleteConnectionSuccess();

        void deleteConnectionFail();
    }
}
