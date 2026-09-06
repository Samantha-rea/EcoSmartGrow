package com.ecosmartgrow.model;

public class GrowthLog {
    private int id;
    private int day;
    private String stage;
    private double size;
    private String color;
    private double healthScore;
    private String timestamp;

    public GrowthLog() {}

    public GrowthLog(int day, String stage, double size, String color, double healthScore) {
        this.day = day;
        this.stage = stage;
        this.size = size;
        this.color = color;
        this.healthScore = healthScore;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public int getDay() { return day; }
    public void setDay(int day) { this.day = day; }

    public String getStage() { return stage; }
    public void setStage(String stage) { this.stage = stage; }

    public double getSize() { return size; }
    public void setSize(double size) { this.size = size; }

    public String getColor() { return color; }
    public void setColor(String color) { this.color = color; }

    public double getHealthScore() { return healthScore; }
    public void setHealthScore(double healthScore) { this.healthScore = healthScore; }

    public String getTimestamp() { return timestamp; }
    public void setTimestamp(String timestamp) { this.timestamp = timestamp; }
}