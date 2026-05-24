package com.careerpilot.dto;

public class ResumeBuildResponse {

    private final String markdown;
    private final boolean aiEnhanced;

    public ResumeBuildResponse(String markdown, boolean aiEnhanced) {
        this.markdown = markdown;
        this.aiEnhanced = aiEnhanced;
    }

    public String getMarkdown() {
        return markdown;
    }

    public boolean isAiEnhanced() {
        return aiEnhanced;
    }
}
