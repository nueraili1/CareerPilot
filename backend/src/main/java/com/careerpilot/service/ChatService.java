package com.careerpilot.service;

import com.careerpilot.dto.ChatMessage;
import com.careerpilot.dto.ChatRequest;
import com.careerpilot.dto.ChatResponse;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class ChatService {

    private final AiClient aiClient;

    public ChatService(AiClient aiClient) {
        this.aiClient = aiClient;
    }

    public ChatResponse chat(ChatRequest request) {
        if (request.getAiConfig() == null || !aiClient.isRuntimeConfigured(request.getAiConfig())) {
            throw new IllegalArgumentException("请先配置 AI_API_KEY、AI_BASE_URL 和 AI_MODEL");
        }

        String systemPrompt = """
                你是 CareerPilot 的 AI 求职助手，擅长 Java 后端、AI 应用、简历优化和模拟面试。
                请基于用户当前简历和目标岗位 JD 回答问题。回答要具体、可执行、适合大学生求职准备。
                如果用户让你改简历，请直接给出可以复制进简历的版本。
                """;

        String context = """
                【当前简历】
                %s

                【目标岗位 JD】
                %s
                """.formatted(limit(request.getResumeText(), 8000), limit(request.getJobDescription(), 4000));

        String answer = aiClient.chat(systemPrompt, context, request.getMessages(), request.getAiConfig());
        return new ChatResponse(answer);
    }

    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value == null ? "" : value;
        }
        return value.substring(0, maxLength);
    }
}
