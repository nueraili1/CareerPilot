package com.careerpilot.dto;

import jakarta.validation.Valid;

public class JobTargetResolveRequest {

    private String companyName;
    private String roleTitle;
    private String description;
    private String imageFileName;
    private String imageDataUrl;

    @Valid
    private AiRuntimeConfig aiConfig;

    public String getCompanyName() {
        return companyName;
    }

    public void setCompanyName(String companyName) {
        this.companyName = companyName;
    }

    public String getRoleTitle() {
        return roleTitle;
    }

    public void setRoleTitle(String roleTitle) {
        this.roleTitle = roleTitle;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageFileName() {
        return imageFileName;
    }

    public void setImageFileName(String imageFileName) {
        this.imageFileName = imageFileName;
    }

    public String getImageDataUrl() {
        return imageDataUrl;
    }

    public void setImageDataUrl(String imageDataUrl) {
        this.imageDataUrl = imageDataUrl;
    }

    public AiRuntimeConfig getAiConfig() {
        return aiConfig;
    }

    public void setAiConfig(AiRuntimeConfig aiConfig) {
        this.aiConfig = aiConfig;
    }
}
