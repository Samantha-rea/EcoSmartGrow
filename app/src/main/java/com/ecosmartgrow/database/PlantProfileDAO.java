package com.ecosmartgrow.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.ecosmartgrow.model.PlantProfile;

import java.util.ArrayList;
import java.util.List;

public class PlantProfileDAO {
    private DatabaseHelper dbHelper;

    public PlantProfileDAO(Context context) {
        this.dbHelper = new DatabaseHelper(context);
    }

    // Insert new plant profile
    public long insertPlantProfile(PlantProfile plant) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_PLANT_NAME, plant.getPlantName());
        values.put(DatabaseHelper.COLUMN_PH_MIN, plant.getPhMin());
        values.put(DatabaseHelper.COLUMN_PH_MAX, plant.getPhMax());
        values.put(DatabaseHelper.COLUMN_EC_MIN, plant.getEcMin());
        values.put(DatabaseHelper.COLUMN_EC_MAX, plant.getEcMax());
        values.put(DatabaseHelper.COLUMN_TEMP_MIN, plant.getTempMin());
        values.put(DatabaseHelper.COLUMN_TEMP_MAX, plant.getTempMax());
        values.put(DatabaseHelper.COLUMN_HUMIDITY_MIN, plant.getHumidityMin());
        values.put(DatabaseHelper.COLUMN_HUMIDITY_MAX, plant.getHumidityMax());
        values.put(DatabaseHelper.COLUMN_GROWTH_STAGE, plant.getGrowthStage());
        values.put(DatabaseHelper.COLUMN_PLANT_HEIGHT, plant.getPlantHeight());
        values.put(DatabaseHelper.COLUMN_DAYS_PLANTED, plant.getDaysPlanted());

        return db.insert(DatabaseHelper.TABLE_PLANT_PROFILES, null, values);
    }

    // Get all plant profiles
    public List<PlantProfile> getAllPlants() {
        List<PlantProfile> plants = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT * FROM " + DatabaseHelper.TABLE_PLANT_PROFILES;
        Cursor cursor = db.rawQuery(query, null);

        while (cursor.moveToNext()) {
            plants.add(cursorToPlantProfile(cursor));
        }
        cursor.close();
        return plants;
    }

    // Get plant by ID
    public PlantProfile getPlantById(int id) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT * FROM " + DatabaseHelper.TABLE_PLANT_PROFILES 
                     + " WHERE " + DatabaseHelper.COLUMN_ID + " = ?";
        String[] args = {String.valueOf(id)};
        Cursor cursor = db.rawQuery(query, args);

        PlantProfile plant = null;
        if (cursor.moveToFirst()) {
            plant = cursorToPlantProfile(cursor);
        }
        cursor.close();
        return plant;
    }

    // Update plant profile
    public int updatePlantProfile(PlantProfile plant) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_PLANT_NAME, plant.getPlantName());
        values.put(DatabaseHelper.COLUMN_PH_MIN, plant.getPhMin());
        values.put(DatabaseHelper.COLUMN_PH_MAX, plant.getPhMax());
        values.put(DatabaseHelper.COLUMN_EC_MIN, plant.getEcMin());
        values.put(DatabaseHelper.COLUMN_EC_MAX, plant.getEcMax());
        values.put(DatabaseHelper.COLUMN_TEMP_MIN, plant.getTempMin());
        values.put(DatabaseHelper.COLUMN_TEMP_MAX, plant.getTempMax());
        values.put(DatabaseHelper.COLUMN_HUMIDITY_MIN, plant.getHumidityMin());
        values.put(DatabaseHelper.COLUMN_HUMIDITY_MAX, plant.getHumidityMax());
        values.put(DatabaseHelper.COLUMN_GROWTH_STAGE, plant.getGrowthStage());
        values.put(DatabaseHelper.COLUMN_PLANT_HEIGHT, plant.getPlantHeight());
        values.put(DatabaseHelper.COLUMN_DAYS_PLANTED, plant.getDaysPlanted());

        String whereClause = DatabaseHelper.COLUMN_ID + " = ?";
        String[] whereArgs = {String.valueOf(plant.getId())};
        return db.update(DatabaseHelper.TABLE_PLANT_PROFILES, values, whereClause, whereArgs);
    }

    // Delete plant profile
    public int deletePlantProfile(int id) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        String whereClause = DatabaseHelper.COLUMN_ID + " = ?";
        String[] whereArgs = {String.valueOf(id)};
        return db.delete(DatabaseHelper.TABLE_PLANT_PROFILES, whereClause, whereArgs);
    }

    // Get plant by name
    public PlantProfile getPlantByName(String name) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String query = "SELECT * FROM " + DatabaseHelper.TABLE_PLANT_PROFILES 
                     + " WHERE " + DatabaseHelper.COLUMN_PLANT_NAME + " = ?";
        String[] args = {name};
        Cursor cursor = db.rawQuery(query, args);

        PlantProfile plant = null;
        if (cursor.moveToFirst()) {
            plant = cursorToPlantProfile(cursor);
        }
        cursor.close();
        return plant;
    }

    // Helper method to convert cursor to PlantProfile
    private PlantProfile cursorToPlantProfile(Cursor cursor) {
        PlantProfile plant = new PlantProfile();
        plant.setId(cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_ID)));
        plant.setPlantName(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_PLANT_NAME)));
        plant.setPhMin(cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.COLUMN_PH_MIN)));
        plant.setPhMax(cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.COLUMN_PH_MAX)));
        plant.setEcMin(cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.COLUMN_EC_MIN)));
        plant.setEcMax(cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.COLUMN_EC_MAX)));
        plant.setTempMin(cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.COLUMN_TEMP_MIN)));
        plant.setTempMax(cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.COLUMN_TEMP_MAX)));
        plant.setHumidityMin(cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.COLUMN_HUMIDITY_MIN)));
        plant.setHumidityMax(cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.COLUMN_HUMIDITY_MAX)));
        plant.setGrowthStage(cursor.getString(cursor.getColumnIndex(DatabaseHelper.COLUMN_GROWTH_STAGE)));
        plant.setPlantHeight(cursor.getDouble(cursor.getColumnIndex(DatabaseHelper.COLUMN_PLANT_HEIGHT)));
        plant.setDaysPlanted(cursor.getInt(cursor.getColumnIndex(DatabaseHelper.COLUMN_DAYS_PLANTED)));
        return plant;
    }
}