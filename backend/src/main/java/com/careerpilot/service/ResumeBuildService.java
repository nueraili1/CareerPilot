package com.careerpilot.service;

import com.careerpilot.dto.ResumeBuildRequest;
import com.careerpilot.dto.ResumeBuildResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class ResumeBuildService {

    private static final Logger log = LoggerFactory.getLogger(ResumeBuildService.class);

    private final AiClient aiClient;

    public ResumeBuildService(AiClient aiClient) {
        this.aiClient = aiClient;
    }

    public ResumeBuildResponse build(ResumeBuildRequest request) {
        String fallback = fallbackMarkdown(request);
        if (!aiClient.isConfigured(request.getAiConfig())) {
            return new ResumeBuildResponse(fallback, false);
        }

        String systemPrompt = """
                你是一名 Java 后端实习求职简历优化专家。
                请根据用户提供的信息，生成一份中文 Markdown 简历。
                要求：内容真实克制、技术导向、适合大学生投 Java 后端 / AI 应用实习；项目经历使用 STAR 写法，突出技术栈、职责和结果。
                只输出 Markdown 正文，不要解释。
                """;
        String userPrompt = """
                姓名：%s
                求职意向：%s
                教育经历：%s
                技能栈：%s
                项目经历：%s
                其他经历：%s
                """.formatted(
                value(request.getName()),
                value(request.getTargetRole()),
                limit(request.getEducation(), 3000),
                limit(request.getSkills(), 3000),
                limit(request.getProjects(), 6000),
                limit(request.getExperience(), 3000));

        try {
            String markdown = aiClient.chat(systemPrompt, userPrompt, request.getAiConfig()).trim();
            return new ResumeBuildResponse(markdown, true);
        } catch (Exception exception) {
            log.warn("AI resume build failed, fallback to local markdown: {}", exception.getMessage());
            return new ResumeBuildResponse(fallback, false);
        }
    }

    private String fallbackMarkdown(ResumeBuildRequest request) {
        return """
                # %s

                **求职意向：** %s

                ## 教育经历

                %s

                ## 技能栈

                %s

                ## 项目经历

                %s

                ## 其他经历

                %s
                """.formatted(
                value(request.getName()),
                value(request.getTargetRole()),
                defaultValue(request.getEducation(), "- 请补充学校、专业、时间、主修课程。"),
                defaultValue(request.getSkills(), "- Java / Spring Boot / MySQL / RESTful API / Vue3 / AI API 调用。"),
                defaultValue(request.getProjects(), "- CareerPilot AI 简历分析与模拟面试平台：负责后端接口、简历解析、AI 调用封装、历史记录持久化和前后端联调。"),
                defaultValue(request.getExperience(), "- 可补充竞赛、社团、实习、兼职中能体现沟通和责任心的经历。"))
                .trim();
    }

    private String value(String value) {
        return StringUtils.hasText(value) ? value.trim() : "";
    }

    private String defaultValue(String value, String fallback) {
        return StringUtils.hasText(value) ? value.trim() : fallback;
    }

    private String limit(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }
}
