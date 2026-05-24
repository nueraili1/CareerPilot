package com.careerpilot.dto;

import java.util.ArrayList;
import java.util.List;

public class InterviewResponse {

    private List<String> questions = new ArrayList<>();
    private String referenceAnswer = "";
    private String feedback = "";

    public List<String> getQuestions() {
        return questions;
    }

    public void setQuestions(List<String> questions) {
        this.questions = questions;
    }

    public String getReferenceAnswer() {
        return referenceAnswer;
    }

    public void setReferenceAnswer(String referenceAnswer) {
        this.referenceAnswer = referenceAnswer;
    }

    public String getFeedback() {
        return feedback;
    }

    public void setFeedback(String feedback) {
        this.feedback = feedback;
    }
}

