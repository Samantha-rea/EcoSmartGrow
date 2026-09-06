package com.ecosmartgrow.repository;

import android.content.Context;

import com.ecosmartgrow.database.SensorLogDAO;
import com.ecosmartgrow.model.SensorLog;

import java.util.List;

public class SensorRepository {
    private SensorLogDAO sensorLogDAO;

    public SensorRepository(Context context) {
        this.sensorLogDAO = new SensorLogDAO(context);
    }

    public long saveReading(SensorLog log) {
        log.setStatus(determineOverallStatus(log));
        return sensorLogDAO.insertSensorLog(log);
    }

    public SensorLog getLatestReading() {
        return sensorLogDAO.getLatestReading();
    }

    public List<SensorLog> getAllReadings() {
        return sensorLogDAO.getAllReadings();
    }

    public List<SensorLog> getLast7DaysReadings() {
        return sensorLogDAO.getLast7DaysReadings();
    }

    public List<SensorLog> getReadingsByDateRange(String start, String end) {
        return sensorLogDAO.getReadingsByDateRange(start, end);
    }

    public void deleteAllReadings() {
        sensorLogDAO.deleteAllReadings();
    }

    public int getReadingsCount() {
        return sensorLogDAO.getReadingsCount();
    }

    private String determineOverallStatus(SensorLog log) {
        boolean phOk = log.getPh() >= 5.8 && log.getPh() <= 6.2;
        boolean ecOk = log.getEc() >= 1.0 && log.getEc() <= 1.4;
        boolean tempOk = log.getTemperature() >= 20 && log.getTemperature() <= 24;
        boolean humidityOk = log.getHumidity() >= 60 && log.getHumidity() <= 70;

        if (phOk && ecOk && tempOk && humidityOk) {
            return "Healthy";
        } else if (!phOk) {
            return "pH Alert";
        } else if (!ecOk) {
            return "EC Alert";
        } else if (!tempOk) {
            return "Temperature Alert";
        } else if (!humidityOk) {
            return "Humidity Alert";
        }
        return "Warning";
    }
}