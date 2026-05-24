package com.careerpilot.dto;

public class ResumeParseResponse {

    private final String fileName;
    private final String text;

    public ResumeParseResponse(String fileName, String text) {
        this.fileName = fileName;
        this.text = text;
    }

    public String getFileName() {
        return fileName;
    }

    public String getText() {
        return text;
    }
}

