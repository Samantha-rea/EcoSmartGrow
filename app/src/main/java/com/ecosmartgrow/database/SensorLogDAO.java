package com.ecosmartgrow.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.ecosmartgrow.model.SensorLog;

import java.util.ArrayList;
import java.util.List;

public class SensorLogDAO {
    private DatabaseHelper dbHelper;

    public SensorLogDAO(Context context) {
        this.dbHelper = new DatabaseHelper(context);
    }

    // Insert new sensor reading
    public long insertSensorLog(SensorLog log) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_PH, log.getPh());
        values.put(DatabaseHelper.COLUMN_EC, log.getEc());
        values.put(DatabaseHelper.COLUMN_TEMPERATURE, log.getTemperature());
        values.put(DatabaseHelper.COLUMN_HUMIDITY, log.getHumidity());
        values.put(DatabaseHelper.COLUMN_WATER_LEVEL, log.getWaterLevel());
        values.put(DatabaseHelper.COLUMN_LIGHT_INTENSITY, log.getLightIntensity());
        values.put(DatabaseHelper.COLUMN_STATUS, log.getStatus());

        return db.insert(DatabaseHelper.TABLE_SENSOR_LOGS, null, values);
    }

    // Get latest reading
    public SensorLog getLatestReading() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT * FROM " + DatabaseHelper.TABLE_SENSOR_LOGS 
                     + " ORDER BY " + DatabaseHelper.COLUMN_TIMESTAMP 
                     + " DESC LIMIT 1";
        Cursor cursor = db.rawQuery(query, null);

        SensorLog log = null;
        if (cursor.moveToFirst()) {
            log = cursorToSensorLog(cursor);
        }
        cursor.close();
        return log;
    }

    // Get all readings
    public List<SensorLog> getAllReadings() {
        List<SensorLog> logs = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT * FROM " + DatabaseHelper.TABLE_SENSOR_LOGS 
                     + " ORDER BY " + DatabaseHelper.COLUMN_TIMESTAMP + " DESC";
        Cursor cursor = db.rawQuery(query, null);

        while (cursor.moveToNext()) {
            logs.add(cursorToSensorLog(cursor));
        }
        cursor.close();
        return logs;
    }

    // Get last 7 days readings
    public List<SensorLog> getLast7DaysReadings() {
        List<SensorLog> logs = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT * FROM " + DatabaseHelper.TABLE_SENSOR_LOGS 
                     + " WHERE " + DatabaseHelper.COLUMN_TIMESTAMP 
                     + " >= datetime('now', '-7 days')"
                     + " ORDER BY " + DatabaseHelper.COLUMN_TIMESTAMP + " ASC";
        Cursor cursor = db.rawQuery(query, null);

        while (cursor.moveToNext()) {
            logs.add(cursorToSensorLog(cursor));
        }
        cursor.close();
        return logs;
    }

    // Get readings by date range
    public List<SensorLog> getReadingsByDateRange(String startDate, String endDate) {
        List<SensorLog> logs = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT * FROM " + DatabaseHelper.TABLE_SENSOR_LOGS 
                     + " WHERE " + DatabaseHelper.COLUMN_TIMESTAMP 
                     + " BETWEEN ? AND ?"
                     + " ORDER BY " + DatabaseHelper.COLUMN_TIMESTAMP + " DESC";
        String[] args = {startDate, endDate};
        Cursor cursor = db.rawQuery(query, args);

        while (cursor.moveToNext()) {
            logs.add(cursorToSensorLog(cursor));
        }
        cursor.close();
        return logs;
    }

    // Delete old readings
    public int deleteOldReadings(int daysToKeep) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String whereClause = DatabaseHelper.COLUMN_TIMESTAMP 
                           + " < datetime('now', '-" + daysToKeep + " days')";
        return db.delete(DatabaseHelper.TABLE_SENSOR_LOGS, whereClause, null);
    }

    // Delete all readings
    public void deleteAllReadings() {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete(DatabaseHelper.TABLE_SENSOR_LOGS, null, null);
    }

    // Get count of readings
    public int getReadingsCount() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_SENSOR_LOGS;
        Cursor cursor = db.rawQuery(query, null);
        cursor.moveToFirst();
        int count = cursor.getInt(0);
        cursor.close();
        return count;
    }

    // Helper method to convert cursor to SensorLog object
    private SensorLog cursorToSensorLog(Cursor cursor) {
        SensorLog log = new SensorLog();
        log.setId(cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_ID)));
        log.setPh(cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.COLUMN_PH)));
        log.setEc(cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.COLUMN_EC)));
        log.setTemperature(cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.COLUMN_TEMPERATURE)));
        log.setHumidity(cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.COLUMN_HUMIDITY)));
        log.setWaterLevel(cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.COLUMN_WATER_LEVEL)));
        log.setLightIntensity(cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.COLUMN_LIGHT_INTENSITY)));
        log.setStatus(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_STATUS)));
        log.setTimestamp(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_TIMESTAMP)));
        return log;
    }
}