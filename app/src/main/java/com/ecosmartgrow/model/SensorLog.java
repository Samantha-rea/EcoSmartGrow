package com.ecosmartgrow.model;

public class SensorLog {
    private int id;
    private double ph;
    private double ec;
    private double temperature;
    private double humidity;
    private double waterLevel;
    private double lightIntensity;
    private double tds;  // ADD THIS
    private String status;
    private String timestamp;

    // Constructor
    public SensorLog() {}

    public SensorLog(double ph, double ec, double temperature, double humidity,
                     double waterLevel, double lightIntensity, double tds) {
        this.ph = ph;
        this.ec = ec;
        this.temperature = temperature;
        this.humidity = humidity;
        this.waterLevel = waterLevel;
        this.lightIntensity = lightIntensity;
        this.tds = tds;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public double getPh() { return ph; }
    public void setPh(double ph) { this.ph = ph; }

    public double getEc() { return ec; }
    public void setEc(double ec) { this.ec = ec; }

    public double getTemperature() { return temperature; }
    public void setTemperature(double temperature) { this.temperature = temperature; }

    public double getHumidity() { return humidity; }
    public void setHumidity(double humidity) { this.humidity = humidity; }

    public double getWaterLevel() { return waterLevel; }
    public void setWaterLevel(double waterLevel) { this.waterLevel = waterLevel; }

    public double getLightIntensity() { return lightIntensity; }
    public void setLightIntensity(double lightIntensity) { this.lightIntensity = lightIntensity; }

    public double getTds() { return tds; }  // ADD THIS
    public void setTds(double tds) { this.tds = tds; }  // ADD THIS

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }

    // Helper method to determine status
    public String determineStatus(double value, double min, double max) {
        if (value >= min && value <= max) {
            return "Normal";
        } else if (value > max) {
            return "High";
        } else {
            return "Low";
        }
    }
}