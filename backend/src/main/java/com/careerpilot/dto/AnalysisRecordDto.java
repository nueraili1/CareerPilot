package com.careerpilot.dto;

import java.time.LocalDateTime;

public class AnalysisRecordDto {

    private Long id;
    private String resumeName;
    private Integer matchScore;
    private String summary;
    private LocalDateTime createdAt;

    public AnalysisRecordDto(Long id, String resumeName, Integer matchScore, String summary, LocalDateTime createdAt) {
        this.id = id;
        this.resumeName = resumeName;
        this.matchScore = matchScore;
        this.summary = summary;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getResumeName() {
        return resumeName;
    }

    public Integer getMatchScore() {
        return matchScore;
    }

    public String getSummary() {
        return summary;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}

