package com.careerpilot.dto;

import jakarta.validation.constraints.NotBlank;

public class InterviewRequest {

    @NotBlank(message = "简历文本不能为空")
    private String resumeText;

    @NotBlank(message = "岗位 JD 不能为空")
    private String jobDescription;

    private String answer;
    private AiRuntimeConfig aiConfig;

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

    public String getAnswer() {
        return answer;
    }

    public void setAnswer(String answer) {
        this.answer = answer;
    }

    public AiRuntimeConfig getAiConfig() {
        return aiConfig;
    }

    public void setAiConfig(AiRuntimeConfig aiConfig) {
        this.aiConfig = aiConfig;
    }
}
