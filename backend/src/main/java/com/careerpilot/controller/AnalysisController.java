package com.careerpilot.controller;

import com.careerpilot.dto.AnalysisRecordDto;
import com.careerpilot.dto.AnalysisRecordDetailDto;
import com.careerpilot.dto.ApiResponse;
import com.careerpilot.dto.AnalyzeRequest;
import com.careerpilot.dto.AnalyzeResponse;
import com.careerpilot.service.AnalysisService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {

    private final AnalysisService analysisService;

    public AnalysisController(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @PostMapping
    public ApiResponse<AnalyzeResponse> analyze(@Valid @RequestBody AnalyzeRequest request) {
        return ApiResponse.ok(analysisService.analyze(request));
    }

    @GetMapping("/records")
    public ApiResponse<List<AnalysisRecordDto>> records() {
        return ApiResponse.ok(analysisService.latestRecords());
    }

    @GetMapping("/records/{id}")
    public ApiResponse<AnalysisRecordDetailDto> recordDetail(@PathVariable Long id) {
        return ApiResponse.ok(analysisService.recordDetail(id));
    }

    @DeleteMapping("/records/{id}")
    public ApiResponse<Void> deleteRecord(@PathVariable Long id) {
        analysisService.deleteRecord(id);
        return ApiResponse.ok(null);
    }
}
