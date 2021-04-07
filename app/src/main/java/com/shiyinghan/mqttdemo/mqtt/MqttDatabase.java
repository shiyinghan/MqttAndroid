package com.shiyinghan.mqttdemo.mqtt;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverter;
import androidx.room.TypeConverters;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.shiyinghan.mqttdemo.mqtt.dao.ConnectionDao;
import com.shiyinghan.mqttdemo.mqtt.dao.SubscriptionDao;
import com.shiyinghan.mqttdemo.mqtt.entity.ConnectionEntity;
import com.shiyinghan.mqttdemo.mqtt.entity.SubscriptionEntity;

import java.util.Calendar;
import java.util.Date;

@Database(entities = {ConnectionEntity.class, SubscriptionEntity.class}, version = 1, exportSchema = false)
@TypeConverters(MqttDatabase.Converters.class)
public abstract class MqttDatabase extends RoomDatabase {

    private static volatile MqttDatabase instance;

    public static MqttDatabase getInstance(Context context) {
        synchronized (MqttDatabase.class) {
            if (instance == null) {
                instance = buildDatabase(context);
            }
        }
        return instance;
    }

    private static MqttDatabase buildDatabase(Context context) {
        return Room.databaseBuilder(context, MqttDatabase.class, "MqttDatabase.db")
                .addCallback(new Callback() {
                    @Override
                    public void onCreate(@NonNull SupportSQLiteDatabase db) {
                        super.onCreate(db);
                        /**
                         * 可以在这里初始化数据库
                         */
                    }
                })
                .build();
    }

    public abstract ConnectionDao getConnectionDao();

    public abstract SubscriptionDao getSubscriptionDao();

    /**
     * Type converters to allow Room to reference complex data types.
     *
     * @author admin
     */
    public class Converters {

        @TypeConverter
        public long calendarToDatestamp(Calendar calendar) {
            return calendar.getTimeInMillis();
        }

        @TypeConverter
        public Calendar datestampToCalendar(long value) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(value);
            return calendar;
        }

        @TypeConverter
        public long dateToDatestamp(Date date) {
            return date.getTime();
        }

        @TypeConverter
        public Date datestampToDater(long value) {
            return new Date(value);
        }
    }
}
