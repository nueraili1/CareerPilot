package com.careerpilot.dto;

public class AiStatusResponse {

    private final boolean configured;
    private final String baseUrl;
    private final String model;

    public AiStatusResponse(boolean configured, String baseUrl, String model) {
        this.configured = configured;
        this.baseUrl = baseUrl;
        this.model = model;
    }

    public boolean isConfigured() {
        return configured;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getModel() {
        return model;
    }
}

