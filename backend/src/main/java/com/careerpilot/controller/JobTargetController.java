package com.careerpilot.controller;

import com.careerpilot.dto.ApiResponse;
import com.careerpilot.dto.JobTargetContext;
import com.careerpilot.dto.JobTargetResolveRequest;
import com.careerpilot.service.JobTargetService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/job-target")
public class JobTargetController {

    private final JobTargetService jobTargetService;

    public JobTargetController(JobTargetService jobTargetService) {
        this.jobTargetService = jobTargetService;
    }

    @PostMapping("/resolve")
    public ApiResponse<JobTargetContext> resolve(@Valid @RequestBody JobTargetResolveRequest request) {
        return ApiResponse.ok(jobTargetService.resolve(request));
    }
}
