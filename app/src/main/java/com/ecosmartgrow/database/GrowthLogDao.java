package com.ecosmartgrow.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.ecosmartgrow.model.GrowthLog;

import java.util.ArrayList;
import java.util.List;

public class GrowthLogDao {
    private DatabaseHelper dbHelper;

    public GrowthLogDao(Context context) {
        this.dbHelper = new DatabaseHelper(context);
    }

    // Insert growth log
    public long insertGrowthLog(GrowthLog log) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_DAY, log.getDay());
        values.put(DatabaseHelper.COLUMN_STAGE, log.getStage());
        values.put(DatabaseHelper.COLUMN_SIZE, log.getSize());
        values.put(DatabaseHelper.COLUMN_COLOR, log.getColor());
        values.put(DatabaseHelper.COLUMN_HEALTH_SCORE, log.getHealthScore());

        return db.insert(DatabaseHelper.TABLE_GROWTH_LOGS, null, values);
    }

    // Get all growth logs
    public List<GrowthLog> getAllGrowthLogs() {
        List<GrowthLog> logs = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT * FROM " + DatabaseHelper.TABLE_GROWTH_LOGS 
                     + " ORDER BY " + DatabaseHelper.COLUMN_DAY + " DESC";
        Cursor cursor = db.rawQuery(query, null);

        while (cursor.moveToNext()) {
            logs.add(cursorToGrowthLog(cursor));
        }
        cursor.close();
        return logs;
    }

    // Get latest growth log
    public GrowthLog getLatestGrowthLog() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT * FROM " + DatabaseHelper.TABLE_GROWTH_LOGS 
                     + " ORDER BY " + DatabaseHelper.COLUMN_DAY + " DESC LIMIT 1";
        Cursor cursor = db.rawQuery(query, null);

        GrowthLog log = null;
        if (cursor.moveToFirst()) {
            log = cursorToGrowthLog(cursor);
        }
        cursor.close();
        return log;
    }

    // Helper method to convert cursor to GrowthLog
    private GrowthLog cursorToGrowthLog(Cursor cursor) {
        GrowthLog log = new GrowthLog();
        log.setId(cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_ID)));
        log.setDay(cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_DAY)));
        log.setStage(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_STAGE)));
        log.setSize(cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.COLUMN_SIZE)));
        log.setColor(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_COLOR)));
        log.setHealthScore(cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.COLUMN_HEALTH_SCORE)));
        log.setTimestamp(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_TIMESTAMP)));
        return log;
    }
}