package com.ecosmartgrow.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {
    private static final String DATABASE_NAME = "ecosmartgrow.db";
    private static final int DATABASE_VERSION = 2;

    // Table Names
    public static final String TABLE_SENSOR_LOGS = "sensor_logs";
    public static final String TABLE_PLANT_PROFILES = "plant_profiles";
    public static final String TABLE_GROWTH_LOGS = "growth_logs";
    public static final String TABLE_SETTINGS = "settings";

    // Common Columns
    public static final String COLUMN_ID = "id";
    public static final String COLUMN_TIMESTAMP = "timestamp";

    // Sensor Logs Columns
    public static final String COLUMN_PH = "ph";
    public static final String COLUMN_EC = "ec";
    public static final String COLUMN_TEMPERATURE = "temperature";
    public static final String COLUMN_HUMIDITY = "humidity";
    public static final String COLUMN_WATER_LEVEL = "water_level";
    public static final String COLUMN_LIGHT_INTENSITY = "light_intensity";
    public static final String COLUMN_STATUS = "status";

    // Plant Profiles Columns
    public static final String COLUMN_PLANT_NAME = "plant_name";
    public static final String COLUMN_PH_MIN = "ph_min";
    public static final String COLUMN_PH_MAX = "ph_max";
    public static final String COLUMN_EC_MIN = "ec_min";
    public static final String COLUMN_EC_MAX = "ec_max";
    public static final String COLUMN_TEMP_MIN = "temp_min";
    public static final String COLUMN_TEMP_MAX = "temp_max";
    public static final String COLUMN_HUMIDITY_MIN = "humidity_min";
    public static final String COLUMN_HUMIDITY_MAX = "humidity_max";
    public static final String COLUMN_GROWTH_STAGE = "growth_stage";
    public static final String COLUMN_PLANT_HEIGHT = "plant_height";
    public static final String COLUMN_DAYS_PLANTED = "days_planted";

    // Growth Logs Columns
    public static final String COLUMN_DAY = "day";
    public static final String COLUMN_STAGE = "stage";
    public static final String COLUMN_SIZE = "size";
    public static final String COLUMN_COLOR = "color";
    public static final String COLUMN_HEALTH_SCORE = "health_score";

    // Settings Columns
    public static final String COLUMN_AUTO_RECORD = "auto_record";
    public static final String COLUMN_RECORD_INTERVAL = "record_interval";
    public static final String COLUMN_START_TIME = "start_time";
    public static final String COLUMN_NOTIFICATIONS = "notifications";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Create sensor_logs table
        String CREATE_SENSOR_TABLE = "CREATE TABLE " + TABLE_SENSOR_LOGS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_TIMESTAMP + " DATETIME DEFAULT CURRENT_TIMESTAMP,"
                + COLUMN_PH + " REAL,"
                + COLUMN_EC + " REAL,"
                + COLUMN_TEMPERATURE + " REAL,"
                + COLUMN_HUMIDITY + " REAL,"
                + COLUMN_WATER_LEVEL + " REAL,"
                + COLUMN_LIGHT_INTENSITY + " REAL,"
                + COLUMN_STATUS + " TEXT"
                + ")";
        db.execSQL(CREATE_SENSOR_TABLE);

        // Create plant_profiles table
        String CREATE_PLANT_TABLE = "CREATE TABLE " + TABLE_PLANT_PROFILES + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_PLANT_NAME + " TEXT,"
                + COLUMN_PH_MIN + " REAL,"
                + COLUMN_PH_MAX + " REAL,"
                + COLUMN_EC_MIN + " REAL,"
                + COLUMN_EC_MAX + " REAL,"
                + COLUMN_TEMP_MIN + " REAL,"
                + COLUMN_TEMP_MAX + " REAL,"
                + COLUMN_HUMIDITY_MIN + " REAL,"
                + COLUMN_HUMIDITY_MAX + " REAL,"
                + COLUMN_GROWTH_STAGE + " TEXT,"
                + COLUMN_PLANT_HEIGHT + " REAL,"
                + COLUMN_DAYS_PLANTED + " INTEGER"
                + ")";
        db.execSQL(CREATE_PLANT_TABLE);

        // Create growth_logs table
        String CREATE_GROWTH_TABLE = "CREATE TABLE " + TABLE_GROWTH_LOGS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_DAY + " INTEGER,"
                + COLUMN_STAGE + " TEXT,"
                + COLUMN_SIZE + " REAL,"
                + COLUMN_COLOR + " TEXT,"
                + COLUMN_HEALTH_SCORE + " REAL,"
                + COLUMN_TIMESTAMP + " DATETIME DEFAULT CURRENT_TIMESTAMP"
                + ")";
        db.execSQL(CREATE_GROWTH_TABLE);

        // Create settings table
        String CREATE_SETTINGS_TABLE = "CREATE TABLE " + TABLE_SETTINGS + "("
                + COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + COLUMN_PH_MIN + " REAL,"
                + COLUMN_PH_MAX + " REAL,"
                + COLUMN_TEMP_MIN + " REAL,"
                + COLUMN_TEMP_MAX + " REAL,"
                + COLUMN_EC_MIN + " REAL,"
                + COLUMN_EC_MAX + " REAL,"
                + COLUMN_HUMIDITY_MIN + " REAL,"
                + COLUMN_HUMIDITY_MAX + " REAL,"
                + COLUMN_AUTO_RECORD + " INTEGER,"
                + COLUMN_RECORD_INTERVAL + " INTEGER,"
                + COLUMN_START_TIME + " TEXT,"
                + COLUMN_NOTIFICATIONS + " INTEGER"
                + ")";
        db.execSQL(CREATE_SETTINGS_TABLE);

        // Insert default settings
        insertDefaultSettings(db);
        insertSampleGrowthLogs(db);
    }

    private void insertDefaultSettings(SQLiteDatabase db) {
        ContentValues values = new ContentValues();
        values.put(COLUMN_PH_MIN, 5.8);
        values.put(COLUMN_PH_MAX, 6.2);
        values.put(COLUMN_TEMP_MIN, 20.0);
        values.put(COLUMN_TEMP_MAX, 24.0);
        values.put(COLUMN_EC_MIN, 1.0);
        values.put(COLUMN_EC_MAX, 1.4);
        values.put(COLUMN_HUMIDITY_MIN, 60.0);
        values.put(COLUMN_HUMIDITY_MAX, 70.0);
        values.put(COLUMN_AUTO_RECORD, 1);
        values.put(COLUMN_RECORD_INTERVAL, 6);
        values.put(COLUMN_START_TIME, "06:00");
        values.put(COLUMN_NOTIFICATIONS, 1);
        db.insert(TABLE_SETTINGS, null, values);
    }

    private void insertSampleGrowthLogs(SQLiteDatabase db) {
        String[][] growthData = {
            {"12", "Vegetative", "18", "Rich Green", "94"},
            {"9", "Vegetative", "15", "Healthy Green", "90"},
            {"6", "Seedling", "12", "Light Green", "85"},
            {"3", "Seedling", "8", "Pale Green", "75"}
        };

        for (String[] data : growthData) {
            ContentValues values = new ContentValues();
            values.put(COLUMN_DAY, Integer.parseInt(data[0]));
            values.put(COLUMN_STAGE, data[1]);
            values.put(COLUMN_SIZE, Double.parseDouble(data[2]));
            values.put(COLUMN_COLOR, data[3]);
            values.put(COLUMN_HEALTH_SCORE, Double.parseDouble(data[4]));
            db.insert(TABLE_GROWTH_LOGS, null, values);
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SENSOR_LOGS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PLANT_PROFILES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_GROWTH_LOGS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SETTINGS);
        onCreate(db);
    }
}