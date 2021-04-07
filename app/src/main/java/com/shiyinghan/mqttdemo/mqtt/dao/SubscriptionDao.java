package com.shiyinghan.mqttdemo.mqtt.dao;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.shiyinghan.mqttdemo.mqtt.entity.SubscriptionEntity;

import java.util.List;

import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;

@Dao
public interface SubscriptionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    public Completable insertSubscription(SubscriptionEntity... connections);

    @Update
    public Completable updateSubscription(SubscriptionEntity... connections);

    @Delete
    public Completable deleteSubscription(SubscriptionEntity... connections);

    @Query("SELECT *  FROM SubscriptionEntity WHERE 1")
    public Observable<List<SubscriptionEntity>> findSubscriptionAll();

    @Query("SELECT *  FROM SubscriptionEntity WHERE clientHandle=:clientHandle")
    public Observable<List<SubscriptionEntity>> findSubscriptionWithClientHandle(String clientHandle);

    @Query("SELECT *  FROM SubscriptionEntity WHERE clientHandle=:clientHandle AND topic=:topic")
    public Single<List<SubscriptionEntity>> findSubscriptionWithClientHandleAndTopic(String clientHandle,String topic);
}
