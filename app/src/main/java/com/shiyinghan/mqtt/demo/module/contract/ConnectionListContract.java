package com.shiyinghan.mqtt.demo.module.contract;

import com.shiyinghan.mqtt.demo.mqtt.entity.ConnectionEntity;

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
