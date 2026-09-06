package com.ecosmartgrow.ml;

public class MLService {
    public PredictionResponse predict(double temperature, double humidity) {
        return new PredictionResponse("ready", 0.0f);
    }
}