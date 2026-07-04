package com.careerpilot.dto;

import java.time.LocalDateTime;

public class AnalysisRecordDetailDto {

    private final Long id;
    private final String resumeName;
    private final String resumeText;
    private final String jobDescription;
    private final JobTargetContext targetContext;
    private final Integer matchScore;
    private final String summary;
    private final AnalyzeResponse result;
    private final LocalDateTime createdAt;

    public AnalysisRecordDetailDto(
            Long id,
            String resumeName,
            String resumeText,
            String jobDescription,
            JobTargetContext targetContext,
            Integer matchScore,
            String summary,
            AnalyzeResponse result,
            LocalDateTime createdAt) {
        this.id = id;
        this.resumeName = resumeName;
        this.resumeText = resumeText;
        this.jobDescription = jobDescription;
        this.targetContext = targetContext;
        this.matchScore = matchScore;
        this.summary = summary;
        this.result = result;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getResumeName() {
        return resumeName;
    }

    public String getResumeText() {
        return resumeText;
    }

    public String getJobDescription() {
        return jobDescription;
    }

    public JobTargetContext getTargetContext() {
        return targetContext;
    }

    public Integer getMatchScore() {
        return matchScore;
    }

    public String getSummary() {
        return summary;
    }

    public AnalyzeResponse getResult() {
        return result;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
