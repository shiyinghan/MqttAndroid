package com.shiyinghan.mqtt.android.service;

import java.util.Iterator;

import org.eclipse.paho.client.mqttv3.MqttMessage;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Implementation of the {@link MessageStore} interface, using a SQLite database
 */
class DatabaseMessageStore implements MessageStore {

    // TAG used for indentify trace data etc.
    private static final String TAG = DatabaseMessageStore.class.getSimpleName();

    // One "private" database column name
    // The other database column names are defined in MqttServiceConstants
    private static final String MTIMESTAMP = "mtimestamp";

    // the name of the table in the database to which we will save messages
    private static final String ARRIVED_MESSAGE_TABLE_NAME = "MqttArrivedMessageTable";

    // the database
    private SQLiteDatabase db = null;

    // a SQLiteOpenHelper specific for this database
    private MQTTDatabaseHelper mqttDb = null;

    /**
     * We need a SQLiteOpenHelper to handle database creation and updating
     */
    private static class MQTTDatabaseHelper extends SQLiteOpenHelper {
        // TAG used for indentify trace data etc.
        private static final String TAG = "MQTTDatabaseHelper";

        private static final String DATABASE_NAME = "mqttAndroidService.db";

        // database version, used to recognise when we need to upgrade
        // (delete and recreate)
        private static final int DATABASE_VERSION = 1;

        /**
         * Constructor.
         *
         * @param context
         */
        public MQTTDatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        /**
         * When the database is (re)created, create our table
         *
         * @param database
         */
        @Override
        public void onCreate(SQLiteDatabase database) {
            String createArrivedTableStatement = "CREATE TABLE "
                    + ARRIVED_MESSAGE_TABLE_NAME + "("
                    + MqttConstants.MESSAGE_ID + " TEXT PRIMARY KEY, "
                    + MqttConstants.CLIENT_HANDLE + " TEXT, "
                    + MqttConstants.DESTINATION_NAME + " TEXT, "
                    + MqttConstants.PAYLOAD + " BLOB, "
                    + MqttConstants.QOS + " INTEGER, "
                    + MqttConstants.RETAINED + " TEXT, "
                    + MqttConstants.DUPLICATE + " TEXT, " + MTIMESTAMP
                    + " INTEGER" + ");";
            Log.d(TAG, "onCreate {" + createArrivedTableStatement + "}");
            try {
                database.execSQL(createArrivedTableStatement);
                Log.d(TAG, "created the table");
            } catch (SQLException e) {
                Log.e(TAG, "onCreate", e);
                throw e;
            }
        }

        /**
         * To upgrade the database, drop and recreate our table
         *
         * @param db         the database
         * @param oldVersion ignored
         * @param newVersion ignored
         */

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.d(TAG, "onUpgrade");
            try {
                db.execSQL("DROP TABLE IF EXISTS " + ARRIVED_MESSAGE_TABLE_NAME);
            } catch (SQLException e) {
                Log.e(TAG, "onUpgrade", e);
                throw e;
            }
            onCreate(db);
            Log.d(TAG, "onUpgrade complete");
        }
    }

    /**
     * Constructor - create a DatabaseMessageStore to store arrived MQTT message
     *
     * @param context a context to use for android calls
     */
    public DatabaseMessageStore(Context context) {
        // Open message database
        mqttDb = new MQTTDatabaseHelper(context);

        // Android documentation suggests that this perhaps
        // could/should be done in another thread, but as the
        // database is only one table, I doubt it matters...

        Log.d(TAG, "DatabaseMessageStore<init> complete");
    }

    /**
     * Store an MQTT message
     *
     * @param clientHandle identifier for the client storing the message
     * @param topic        The topic on which the message was published
     * @param message      the arrived MQTT message
     * @return an identifier for the message, so that it can be removed when appropriate
     */
    @Override
    public String storeArrived(String clientHandle, String topic,
                               MqttMessage message) {

        db = mqttDb.getWritableDatabase();

        Log.d(TAG, "storeArrived{" + clientHandle + "}, {"
                + message.toString() + "}");

        byte[] payload = message.getPayload();
        int qos = message.getQos();
        boolean retained = message.isRetained();
        boolean duplicate = message.isDuplicate();

        ContentValues values = new ContentValues();
        String id = java.util.UUID.randomUUID().toString();
        values.put(MqttConstants.MESSAGE_ID, id);
        values.put(MqttConstants.CLIENT_HANDLE, clientHandle);
        values.put(MqttConstants.DESTINATION_NAME, topic);
        values.put(MqttConstants.PAYLOAD, payload);
        values.put(MqttConstants.QOS, qos);
        values.put(MqttConstants.RETAINED, retained);
        values.put(MqttConstants.DUPLICATE, duplicate);
        values.put(MTIMESTAMP, System.currentTimeMillis());
        try {
            db.insertOrThrow(ARRIVED_MESSAGE_TABLE_NAME, null, values);
        } catch (SQLException e) {
            Log.e(TAG, "onUpgrade", e);
            throw e;
        }
        int count = getArrivedRowCount(clientHandle);
        Log.d(TAG,
                "storeArrived: inserted message with id of {"
                        + id
                        + "} - Number of messages in database for this clientHandle = "
                        + count);
        return id;
    }

    private int getArrivedRowCount(String clientHandle) {
        int count = 0;
        String[] projection = {
                MqttConstants.MESSAGE_ID,
        };
        String selection = MqttConstants.CLIENT_HANDLE + "=?";
        String[] selectionArgs = new String[1];
        selectionArgs[0] = clientHandle;
        Cursor c = db.query(
                ARRIVED_MESSAGE_TABLE_NAME, // Table Name
                projection, // The columns to return;
                selection, // Columns for WHERE Clause
                selectionArgs, // The values for the WHERE Cause
                null,  //Don't group the rows
                null,  // Don't filter by row groups
                null   // The sort order
        );

        if (c.moveToFirst()) {
            count = c.getInt(0);
        }
        c.close();
        return count;
    }

    /**
     * Delete an MQTT message.
     *
     * @param clientHandle identifier for the client which stored the message
     * @param id           the identifying string returned when the message was stored
     * @return true if the message was found and deleted
     */
    @Override
    public boolean discardArrived(String clientHandle, String id) {

        db = mqttDb.getWritableDatabase();

        Log.d(TAG, "discardArrived{" + clientHandle + "}, {"
                + id + "}");
        int rows;
        String[] selectionArgs = new String[2];
        selectionArgs[0] = id;
        selectionArgs[1] = clientHandle;

        try {
            rows = db.delete(ARRIVED_MESSAGE_TABLE_NAME,
                    MqttConstants.MESSAGE_ID + "=? AND "
                            + MqttConstants.CLIENT_HANDLE + "=?",
                    selectionArgs);
        } catch (SQLException e) {
            Log.e(TAG, "discardArrived", e);
            throw e;
        }
        if (rows != 1) {
            Log.e(TAG,
                    "discardArrived - Error deleting message {" + id
                            + "} from database: Rows affected = " + rows);
            return false;
        }
        int count = getArrivedRowCount(clientHandle);
        Log.d(TAG,
                "discardArrived - Message deleted successfully. - messages in db for this clientHandle "
                        + count);
        return true;
    }

    /**
     * Get an iterator over all messages stored (optionally for a specific client)
     *
     * @param clientHandle identifier for the client.<br>
     *                     If null, all messages are retrieved
     * @return iterator of all the arrived MQTT messages
     */
    @Override
    public Iterator<StoredMessage> getAllArrivedMessages(
            final String clientHandle) {
        return new Iterator<StoredMessage>() {
            private Cursor c;
            private boolean hasNext;
            private final String[] selectionArgs = {
                    clientHandle,
            };


            {
                db = mqttDb.getWritableDatabase();
                // anonymous initialiser to start a suitable query
                // and position at the first row, if one exists
                if (clientHandle == null) {
                    c = db.query(ARRIVED_MESSAGE_TABLE_NAME,
                            null,
                            null,
                            null,
                            null,
                            null,
                            "mtimestamp ASC");
                } else {
                    c = db.query(ARRIVED_MESSAGE_TABLE_NAME,
                            null,
                            MqttConstants.CLIENT_HANDLE + "=?",
                            selectionArgs,
                            null,
                            null,
                            "mtimestamp ASC");
                }
                hasNext = c.moveToFirst();
            }

            @Override
            public boolean hasNext() {
                if (!hasNext) {
                    c.close();
                }
                return hasNext;
            }

            @Override
            public StoredMessage next() {
                String messageId = c.getString(c
                        .getColumnIndex(MqttConstants.MESSAGE_ID));
                String clientHandle = c.getString(c
                        .getColumnIndex(MqttConstants.CLIENT_HANDLE));
                String topic = c.getString(c
                        .getColumnIndex(MqttConstants.DESTINATION_NAME));
                byte[] payload = c.getBlob(c
                        .getColumnIndex(MqttConstants.PAYLOAD));
                int qos = c.getInt(c.getColumnIndex(MqttConstants.QOS));
                boolean retained = Boolean.parseBoolean(c.getString(c
                        .getColumnIndex(MqttConstants.RETAINED)));
                boolean dup = Boolean.parseBoolean(c.getString(c
                        .getColumnIndex(MqttConstants.DUPLICATE)));

                // build the result
                MqttMessageHack message = new MqttMessageHack(payload);
                message.setQos(qos);
                message.setRetained(retained);
                message.setDuplicate(dup);

                // move on
                hasNext = c.moveToNext();
                return new DbStoredData(messageId, clientHandle, topic, message);
            }

            @Override
            public void remove() {
                throw new UnsupportedOperationException();
            }

            /* (non-Javadoc)
             * @see java.lang.Object#finalize()
             */
            @Override
            protected void finalize() throws Throwable {
                c.close();
                super.finalize();
            }

        };
    }

    /**
     * Delete all messages (optionally for a specific client)
     *
     * @param clientHandle identifier for the client.<br>
     *                     If null, all messages are deleted
     */
    @Override
    public void clearArrivedMessages(String clientHandle) {

        db = mqttDb.getWritableDatabase();
        String[] selectionArgs = new String[1];
        selectionArgs[0] = clientHandle;

        int rows = 0;
        if (clientHandle == null) {
            Log.d(TAG,
                    "clearArrivedMessages: clearing the table");
            rows = db.delete(ARRIVED_MESSAGE_TABLE_NAME, null, null);
        } else {
            Log.d(TAG,
                    "clearArrivedMessages: clearing the table of "
                            + clientHandle + " messages");
            rows = db.delete(ARRIVED_MESSAGE_TABLE_NAME,
                    MqttConstants.CLIENT_HANDLE + "=?",
                    selectionArgs);

        }
        Log.d(TAG, "clearArrivedMessages: rows affected = "
                + rows);
    }

    private class DbStoredData implements StoredMessage {
        private String messageId;
        private String clientHandle;
        private String topic;
        private MqttMessage message;

        DbStoredData(String messageId, String clientHandle, String topic,
                     MqttMessage message) {
            this.messageId = messageId;
            this.topic = topic;
            this.message = message;
        }

        @Override
        public String getMessageId() {
            return messageId;
        }

        @Override
        public String getClientHandle() {
            return clientHandle;
        }

        @Override
        public String getTopic() {
            return topic;
        }

        @Override
        public MqttMessage getMessage() {
            return message;
        }
    }

    /**
     * A way to get at the "setDuplicate" method of MqttMessage
     */
    private class MqttMessageHack extends MqttMessage {

        public MqttMessageHack(byte[] payload) {
            super(payload);
        }

        @Override
        protected void setDuplicate(boolean dup) {
            super.setDuplicate(dup);
        }
    }

    @Override
    public void close() {
        if (this.db != null) {
            this.db.close();
        }
    }

}