package com.ecosmartgrow.model;

public class Settings {
    private int id;
    private double phMin;
    private double phMax;
    private double tempMin;
    private double tempMax;
    private double ecMin;
    private double ecMax;
    private double humidityMin;
    private double humidityMax;
    private int autoRecord;
    private int recordInterval;
    private String startTime;
    private boolean notificationsEnabled;

    public Settings() {
        // Default values
        this.phMin = 5.8;
        this.phMax = 6.2;
        this.tempMin = 20.0;
        this.tempMax = 24.0;
        this.ecMin = 1.0;
        this.ecMax = 1.4;
        this.humidityMin = 60.0;
        this.humidityMax = 70.0;
        this.autoRecord = 1;
        this.recordInterval = 6;
        this.startTime = "06:00";
        this.notificationsEnabled = true;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public double getPhMin() { return phMin; }
    public void setPhMin(double phMin) { this.phMin = phMin; }

    public double getPhMax() { return phMax; }
    public void setPhMax(double phMax) { this.phMax = phMax; }

    public double getTempMin() { return tempMin; }
    public void setTempMin(double tempMin) { this.tempMin = tempMin; }

    public double getTempMax() { return tempMax; }
    public void setTempMax(double tempMax) { this.tempMax = tempMax; }

    public double getEcMin() { return ecMin; }
    public void setEcMin(double ecMin) { this.ecMin = ecMin; }

    public double getEcMax() { return ecMax; }
    public void setEcMax(double ecMax) { this.ecMax = ecMax; }

    public double getHumidityMin() { return humidityMin; }
    public void setHumidityMin(double humidityMin) { this.humidityMin = humidityMin; }

    public double getHumidityMax() { return humidityMax; }
    public void setHumidityMax(double humidityMax) { this.humidityMax = humidityMax; }

    public int getAutoRecord() { return autoRecord; }
    public void setAutoRecord(int autoRecord) { this.autoRecord = autoRecord; }

    public int getRecordInterval() { return recordInterval; }
    public void setRecordInterval(int recordInterval) { this.recordInterval = recordInterval; }

    public String getStartTime() { return startTime; }
    public void setStartTime(String startTime) { this.startTime = startTime; }

    public boolean isNotificationsEnabled() { return notificationsEnabled; }
    public void setNotificationsEnabled(boolean notificationsEnabled) { 
        this.notificationsEnabled = notificationsEnabled; 
    }
}