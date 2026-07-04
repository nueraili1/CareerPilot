package com.careerpilot.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;

public class AnalyzeRequest {

    private String resumeName;

    @Valid
    private AiRuntimeConfig aiConfig;

    @NotBlank(message = "简历文本不能为空")
    private String resumeText;

    private String jobDescription;

    @Valid
    private JobTargetContext targetContext;

    public String getResumeName() {
        return resumeName;
    }

    public void setResumeName(String resumeName) {
        this.resumeName = resumeName;
    }

    public AiRuntimeConfig getAiConfig() {
        return aiConfig;
    }

    public void setAiConfig(AiRuntimeConfig aiConfig) {
        this.aiConfig = aiConfig;
    }

    public String getResumeText() {
        return resumeText;
    }

    public void setResumeText(String resumeText) {
        this.resumeText = resumeText;
    }

    public String getJobDescription() {
        return jobDescription;
    }

    public void setJobDescription(String jobDescription) {
        this.jobDescription = jobDescription;
    }

    public JobTargetContext getTargetContext() {
        return targetContext;
    }

    public void setTargetContext(JobTargetContext targetContext) {
        this.targetContext = targetContext;
    }
}
