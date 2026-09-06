package com.ecosmartgrow.repository;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.ecosmartgrow.database.DatabaseHelper;
import com.ecosmartgrow.model.GrowthLog;

import java.util.ArrayList;
import java.util.List;

public class GrowthLogRepository {
    private DatabaseHelper dbHelper;

    public GrowthLogRepository(Context context) {
        this.dbHelper = new DatabaseHelper(context);
    }

    public long saveGrowthLog(GrowthLog log) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("day", log.getDay());
        values.put("stage", log.getStage());
        values.put("size", log.getSize());
        values.put("color", log.getColor());
        values.put("health_score", log.getHealthScore());
        values.put("timestamp", log.getTimestamp());

        long result = db.insert("growth_logs", null, values);
        db.close();
        return result;
    }

    public List<GrowthLog> getAllGrowthLogs() {
        List<GrowthLog> logs = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.query("growth_logs", null, null, null, null, null, "day DESC");

        if (cursor.moveToFirst()) {
            do {
                GrowthLog log = new GrowthLog();
                log.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
                log.setDay(cursor.getInt(cursor.getColumnIndexOrThrow("day")));
                log.setStage(cursor.getString(cursor.getColumnIndexOrThrow("stage")));
                log.setSize(cursor.getDouble(cursor.getColumnIndexOrThrow("size")));
                log.setColor(cursor.getString(cursor.getColumnIndexOrThrow("color")));
                log.setHealthScore(cursor.getDouble(cursor.getColumnIndexOrThrow("health_score")));
                log.setTimestamp(cursor.getString(cursor.getColumnIndexOrThrow("timestamp")));
                logs.add(log);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return logs;
    }

    public GrowthLog getLatestGrowthLog() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        Cursor cursor = db.query("growth_logs", null, null, null, null, null, "day DESC", "1");

        GrowthLog log = null;
        if (cursor.moveToFirst()) {
            log = new GrowthLog();
            log.setId(cursor.getInt(cursor.getColumnIndexOrThrow("id")));
            log.setDay(cursor.getInt(cursor.getColumnIndexOrThrow("day")));
            log.setStage(cursor.getString(cursor.getColumnIndexOrThrow("stage")));
            log.setSize(cursor.getDouble(cursor.getColumnIndexOrThrow("size")));
            log.setColor(cursor.getString(cursor.getColumnIndexOrThrow("color")));
            log.setHealthScore(cursor.getDouble(cursor.getColumnIndexOrThrow("health_score")));
            log.setTimestamp(cursor.getString(cursor.getColumnIndexOrThrow("timestamp")));
        }

        cursor.close();
        db.close();
        return log;
    }
}