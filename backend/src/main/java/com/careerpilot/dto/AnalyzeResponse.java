package com.careerpilot.dto;

import java.util.ArrayList;
import java.util.List;

public class AnalyzeResponse {

    private Integer matchScore = 0;
    private String summary = "";
    private List<String> strengths = new ArrayList<>();
    private List<String> gaps = new ArrayList<>();
    private List<String> suggestions = new ArrayList<>();
    private List<String> projectRewrite = new ArrayList<>();
    private List<String> interviewQuestions = new ArrayList<>();

    public Integer getMatchScore() {
        return matchScore;
    }

    public void setMatchScore(Integer matchScore) {
        this.matchScore = matchScore;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public List<String> getStrengths() {
        return strengths;
    }

    public void setStrengths(List<String> strengths) {
        this.strengths = strengths;
    }

    public List<String> getGaps() {
        return gaps;
    }

    public void setGaps(List<String> gaps) {
        this.gaps = gaps;
    }

    public List<String> getSuggestions() {
        return suggestions;
    }

    public void setSuggestions(List<String> suggestions) {
        this.suggestions = suggestions;
    }

    public List<String> getProjectRewrite() {
        return projectRewrite;
    }

    public void setProjectRewrite(List<String> projectRewrite) {
        this.projectRewrite = projectRewrite;
    }

    public List<String> getInterviewQuestions() {
        return interviewQuestions;
    }

    public void setInterviewQuestions(List<String> interviewQuestions) {
        this.interviewQuestions = interviewQuestions;
    }
}

