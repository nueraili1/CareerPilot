package com.careerpilot.controller;

import com.careerpilot.dto.ApiResponse;
import com.careerpilot.dto.AiStatusResponse;
import com.careerpilot.service.AiClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
public class AiController {

    private final AiClient aiClient;

    public AiController(AiClient aiClient) {
        this.aiClient = aiClient;
    }

    @GetMapping("/status")
    public ApiResponse<AiStatusResponse> status() {
        return ApiResponse.ok(new AiStatusResponse(aiClient.isConfigured(), aiClient.getBaseUrl(), aiClient.getModel()));
    }
}
