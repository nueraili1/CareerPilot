package com.careerpilot.service;

import com.careerpilot.dto.InterviewRequest;
import com.careerpilot.dto.InterviewResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class InterviewService {

    private static final Logger log = LoggerFactory.getLogger(InterviewService.class);

    private final AiClient aiClient;
    private final ObjectMapper objectMapper;

    public InterviewService(AiClient aiClient, ObjectMapper objectMapper) {
        this.aiClient = aiClient;
        this.objectMapper = objectMapper;
    }

    public InterviewResponse generateQuestions(InterviewRequest request) {
        if (!aiClient.isConfigured(request.getAiConfig())) {
            return mockQuestions();
        }

        String systemPrompt = """
                你是一名 Java 后端和 AI 应用方向面试官。请基于简历和岗位 JD 生成模拟面试内容。
                必须只返回 JSON，不要输出 Markdown。
                JSON 字段为：questions(string[]), referenceAnswer(string), feedback(string)。
                使用中文，问题要包含项目、Java、数据库、AI 接口调用和工程部署。
                """;

        String userPrompt = """
                【简历】
                %s

                【岗位 JD】
                %s

                【候选人回答】
                %s
                """.formatted(request.getResumeText(), request.getJobDescription(), request.getAnswer());

        try {
            String content = aiClient.chat(systemPrompt, userPrompt, request.getAiConfig());
            return objectMapper.readValue(extractJson(content), InterviewResponse.class);
        } catch (Exception exception) {
            log.warn("AI interview generation failed, fallback to local result: {}", exception.getMessage());
            InterviewResponse response = mockQuestions();
            response.setFeedback("AI 返回内容解析失败，当前展示本地兜底面试题。");
            return response;
        }
    }

    private InterviewResponse mockQuestions() {
        InterviewResponse response = new InterviewResponse();
        response.setQuestions(List.of(
                "请介绍你在 CareerPilot 中负责的后端模块。",
                "简历文件上传后，后端如何提取 PDF 或 DOCX 文本？",
                "你如何设计 AI 分析接口，避免前端直接暴露 API Key？",
                "如果模型接口超时或返回非 JSON，你会如何兜底？",
                "JPA 实体 AnalysisRecord 的字段设计考虑了哪些演示需求？"
        ));
        response.setReferenceAnswer("可以从项目背景、接口设计、文档解析、AI 调用封装、数据持久化和异常兜底六个角度回答。");
        response.setFeedback("回答时建议用一次完整请求链路串起来，不要只罗列技术名词。");
        return response;
    }

    private String extractJson(String content) {
        if (content == null) {
            throw new IllegalArgumentException("AI content is empty");
        }

        String cleaned = content.trim();
        if (cleaned.startsWith("```")) {
            cleaned = cleaned.replaceFirst("^```json\\s*", "")
                    .replaceFirst("^```\\s*", "")
                    .replaceFirst("\\s*```$", "")
                    .trim();
        }

        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return cleaned.substring(start, end + 1);
        }
        return cleaned;
    }
}
