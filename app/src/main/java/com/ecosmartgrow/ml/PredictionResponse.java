package com.ecosmartgrow.ml;

public class PredictionResponse {
    private final String status;
    private final float confidence;

    public PredictionResponse(String status, float confidence) {
        this.status = status;
        this.confidence = confidence;
    }

    public String getStatus() {
        return status;
    }

    public float getConfidence() {
        return confidence;
    }
}