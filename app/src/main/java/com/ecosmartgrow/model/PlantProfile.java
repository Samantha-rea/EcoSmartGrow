package com.ecosmartgrow.model;

public class PlantProfile {
    private int id;
    private String plantName;
    private double phMin;
    private double phMax;
    private double ecMin;
    private double ecMax;
    private double tempMin;
    private double tempMax;
    private double humidityMin;
    private double humidityMax;
    private String growthStage;
    private double plantHeight;
    private int daysPlanted;

    // Constructor
    public PlantProfile() {}

    public PlantProfile(String plantName, double phMin, double phMax, double ecMin, 
                        double ecMax, double tempMin, double tempMax, 
                        double humidityMin, double humidityMax) {
        this.plantName = plantName;
        this.phMin = phMin;
        this.phMax = phMax;
        this.ecMin = ecMin;
        this.ecMax = ecMax;
        this.tempMin = tempMin;
        this.tempMax = tempMax;
        this.humidityMin = humidityMin;
        this.humidityMax = humidityMax;
        this.growthStage = "Seedling";
        this.daysPlanted = 1;
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getPlantName() { return plantName; }
    public void setPlantName(String plantName) { this.plantName = plantName; }

    public double getPhMin() { return phMin; }
    public void setPhMin(double phMin) { this.phMin = phMin; }

    public double getPhMax() { return phMax; }
    public void setPhMax(double phMax) { this.phMax = phMax; }

    public double getEcMin() { return ecMin; }
    public void setEcMin(double ecMin) { this.ecMin = ecMin; }

    public double getEcMax() { return ecMax; }
    public void setEcMax(double ecMax) { this.ecMax = ecMax; }

    public double getTempMin() { return tempMin; }
    public void setTempMin(double tempMin) { this.tempMin = tempMin; }

    public double getTempMax() { return tempMax; }
    public void setTempMax(double tempMax) { this.tempMax = tempMax; }

    public double getHumidityMin() { return humidityMin; }
    public void setHumidityMin(double humidityMin) { this.humidityMin = humidityMin; }

    public double getHumidityMax() { return humidityMax; }
    public void setHumidityMax(double humidityMax) { this.humidityMax = humidityMax; }

    public String getGrowthStage() { return growthStage; }
    public void setGrowthStage(String growthStage) { this.growthStage = growthStage; }

    public double getPlantHeight() { return plantHeight; }
    public void setPlantHeight(double plantHeight) { this.plantHeight = plantHeight; }

    public int getDaysPlanted() { return daysPlanted; }
    public void setDaysPlanted(int daysPlanted) { this.daysPlanted = daysPlanted; }
}