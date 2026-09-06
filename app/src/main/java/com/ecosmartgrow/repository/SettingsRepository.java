package com.ecosmartgrow.repository;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.ecosmartgrow.model.Settings;

public class SettingsRepository {
    private static final String TAG = "SettingsRepository";
    private DatabaseHelper dbHelper;
    private Context context;

    public SettingsRepository(Context context) {
        this.context = context;
        dbHelper = new DatabaseHelper(context);
    }

    public boolean saveSettings(Settings settings) {
        SQLiteDatabase db = null;
        try {
            db = dbHelper.getWritableDatabase();

            // Clear existing settings (keep only latest)
            db.delete("settings", null, null);

            ContentValues values = new ContentValues();
            values.put("ph_min", settings.getPhMin());
            values.put("ph_max", settings.getPhMax());
            values.put("ec_min", settings.getEcMin());
            values.put("ec_max", settings.getEcMax());
            values.put("temp_min", settings.getTempMin());
            values.put("temp_max", settings.getTempMax());
            values.put("humidity_min", settings.getHumidityMin());
            values.put("humidity_max", settings.getHumidityMax());
            values.put("start_time", settings.getStartTime());
            values.put("record_interval", settings.getRecordInterval());
            values.put("auto_record", settings.getAutoRecord());
            values.put("notifications", settings.isNotificationsEnabled() ? 1 : 0);

            long result = db.insert("settings", null, values);
            Log.d(TAG, "Settings saved: " + result);
            return result != -1;
        } catch (Exception e) {
            Log.e(TAG, "Error saving settings: " + e.getMessage());
            e.printStackTrace();
            return false;
        } finally {
            if (db != null && db.isOpen()) {
                try {
                    db.close();
                } catch (Exception e) {
                    Log.e(TAG, "Error closing database: " + e.getMessage());
                }
            }
        }
    }

    public Settings getLatestSettings() {
        SQLiteDatabase db = null;
        Cursor cursor = null;
        try {
            db = dbHelper.getReadableDatabase();
            cursor = db.query("settings", null, null, null, null, null, null, "1");

            if (cursor != null && cursor.moveToFirst()) {
                Settings settings = new Settings();
                try {
                    settings.setId(cursor.getInt(cursor.getColumnIndex("id")));
                    settings.setPhMin(cursor.getDouble(cursor.getColumnIndex("ph_min")));
                    settings.setPhMax(cursor.getDouble(cursor.getColumnIndex("ph_max")));
                    settings.setEcMin(cursor.getDouble(cursor.getColumnIndex("ec_min")));
                    settings.setEcMax(cursor.getDouble(cursor.getColumnIndex("ec_max")));
                    settings.setTempMin(cursor.getDouble(cursor.getColumnIndex("temp_min")));
                    settings.setTempMax(cursor.getDouble(cursor.getColumnIndex("temp_max")));
                    settings.setHumidityMin(cursor.getDouble(cursor.getColumnIndex("humidity_min")));
                    settings.setHumidityMax(cursor.getDouble(cursor.getColumnIndex("humidity_max")));
                    settings.setStartTime(cursor.getString(cursor.getColumnIndex("start_time")));
                    settings.setRecordInterval(cursor.getInt(cursor.getColumnIndex("record_interval")));
                    settings.setAutoRecord(cursor.getInt(cursor.getColumnIndex("auto_record")));
                    settings.setNotificationsEnabled(cursor.getInt(cursor.getColumnIndex("notifications")) == 1);
                } catch (Exception e) {
                    Log.e(TAG, "Error reading cursor data: " + e.getMessage());
                }
                return settings;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error loading settings: " + e.getMessage());
            e.printStackTrace();
        } finally {
            if (cursor != null) {
                try {
                    cursor.close();
                } catch (Exception e) {
                    Log.e(TAG, "Error closing cursor: " + e.getMessage());
                }
            }
            if (db != null && db.isOpen()) {
                try {
                    db.close();
                } catch (Exception e) {
                    Log.e(TAG, "Error closing database: " + e.getMessage());
                }
            }
        }
        return null;
    }

    private static class DatabaseHelper extends SQLiteOpenHelper {
        private static final String DATABASE_NAME = "ecosmartgrow.db";
        private static final int DATABASE_VERSION = 2; // Incremented version

        public DatabaseHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
        }

        @Override
        public void onCreate(SQLiteDatabase db) {
            try {
                String createTable = "CREATE TABLE IF NOT EXISTS settings (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                        "ph_min REAL," +
                        "ph_max REAL," +
                        "ec_min REAL," +
                        "ec_max REAL," +
                        "temp_min REAL," +
                        "temp_max REAL," +
                        "humidity_min REAL," +
                        "humidity_max REAL," +
                        "start_time TEXT," +
                        "record_interval INTEGER," +
                        "auto_record INTEGER," +
                        "notifications INTEGER" +
                        ")";
                db.execSQL(createTable);
                Log.d("DatabaseHelper", "Settings table created successfully");
            } catch (Exception e) {
                Log.e("DatabaseHelper", "Error creating table: " + e.getMessage());
                e.printStackTrace();
            }
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            try {
                db.execSQL("DROP TABLE IF EXISTS settings");
                onCreate(db);
                Log.d("DatabaseHelper", "Database upgraded from " + oldVersion + " to " + newVersion);
            } catch (Exception e) {
                Log.e("DatabaseHelper", "Error upgrading database: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
}