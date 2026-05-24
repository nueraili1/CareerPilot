package com.careerpilot.controller;

import com.careerpilot.dto.ApiResponse;
import com.careerpilot.dto.InterviewRequest;
import com.careerpilot.dto.InterviewResponse;
import com.careerpilot.service.InterviewService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/interview")
public class InterviewController {

    private final InterviewService interviewService;

    public InterviewController(InterviewService interviewService) {
        this.interviewService = interviewService;
    }

    @PostMapping("/questions")
    public ApiResponse<InterviewResponse> questions(@Valid @RequestBody InterviewRequest request) {
        return ApiResponse.ok(interviewService.generateQuestions(request));
    }
}
