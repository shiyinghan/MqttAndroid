package com.shiyinghan.mqttdemo.mqtt.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.shiyinghan.mqttdemo.mqtt.entity.ConnectionEntity;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Observable;

@Dao
public interface ConnectionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public Completable insertConnection(ConnectionEntity... connections);

    @Update
    public Completable updateConnection(ConnectionEntity... connections);

    @Delete
    public Completable deleteConnection(ConnectionEntity... connections);

    @Query("SELECT *  FROM ConnectionEntity WHERE 1")
    public Observable<List<ConnectionEntity>> findConnectionAll();
}
