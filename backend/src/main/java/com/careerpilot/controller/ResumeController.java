package com.careerpilot.controller;

import com.careerpilot.dto.ApiResponse;
import com.careerpilot.dto.ResumeBuildRequest;
import com.careerpilot.dto.ResumeBuildResponse;
import com.careerpilot.dto.ResumeParseResponse;
import com.careerpilot.service.ResumeBuildService;
import com.careerpilot.service.ResumeParseService;
import jakarta.validation.Valid;
import java.io.IOException;
import org.apache.tika.exception.TikaException;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/resumes")
public class ResumeController {

    private final ResumeParseService resumeParseService;
    private final ResumeBuildService resumeBuildService;

    public ResumeController(ResumeParseService resumeParseService, ResumeBuildService resumeBuildService) {
        this.resumeParseService = resumeParseService;
        this.resumeBuildService = resumeBuildService;
    }

    @PostMapping("/parse")
    public ApiResponse<ResumeParseResponse> parse(@RequestParam("file") MultipartFile file) throws IOException, TikaException {
        return ApiResponse.ok(resumeParseService.parse(file));
    }

    @PostMapping("/build")
    public ApiResponse<ResumeBuildResponse> build(@Valid @RequestBody ResumeBuildRequest request) {
        return ApiResponse.ok(resumeBuildService.build(request));
    }
}
