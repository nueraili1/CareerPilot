package com.careerpilot.service;

import com.careerpilot.dto.AnalysisRecordDetailDto;
import com.careerpilot.dto.AnalysisRecordDto;
import com.careerpilot.dto.AnalyzeRequest;
import com.careerpilot.dto.AnalyzeResponse;
import com.careerpilot.dto.JobTargetContext;
import com.careerpilot.model.AnalysisRecord;
import com.careerpilot.repository.AnalysisRecordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AnalysisService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisService.class);

    private final AiClient aiClient;
    private final ObjectMapper objectMapper;
    private final AnalysisRecordRepository recordRepository;
    private final AuthContext authContext;
    private final JobTargetService jobTargetService;

    public AnalysisService(
            AiClient aiClient,
            ObjectMapper objectMapper,
            AnalysisRecordRepository recordRepository,
            AuthContext authContext,
            JobTargetService jobTargetService) {
        this.aiClient = aiClient;
        this.objectMapper = objectMapper;
        this.recordRepository = recordRepository;
        this.authContext = authContext;
        this.jobTargetService = jobTargetService;
    }

    public AnalyzeResponse analyze(AnalyzeRequest request) {
        JobTargetContext targetContext = jobTargetService.enrichForPrompt(
                request.getJobDescription(), request.getTargetContext(), request.getAiConfig());
        String effectiveJobDescription = jobTargetService.buildEffectiveJobDescription(request.getJobDescription(), targetContext);
        if (!StringUtils.hasText(effectiveJobDescription)) {
            throw new IllegalArgumentException("请至少填写岗位描述、公司岗位信息或上传岗位截图");
        }

        AnalyzeResponse response = aiClient.isConfigured(request.getAiConfig())
                ? analyzeWithAi(request, effectiveJobDescription)
                : mockAnalyze();
        saveRecord(request, targetContext, effectiveJobDescription, response);
        return response;
    }

    public List<AnalysisRecordDto> latestRecords() {
        List<AnalysisRecord> records = authContext.currentUser()
                .map(user -> recordRepository.findTop10ByUserIdOrderByCreatedAtDesc(user.userId()))
                .orElseGet(recordRepository::findTop10ByUserIdIsNullOrderByCreatedAtDesc);
        return records.stream()
                .map(record -> new AnalysisRecordDto(
                        record.getId(),
                        record.getResumeName(),
                        record.getMatchScore(),
                        record.getSummary(),
                        record.getCreatedAt()))
                .toList();
    }

    public AnalysisRecordDetailDto recordDetail(Long id) {
        AnalysisRecord record = recordRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("分析记录不存在"));
        checkRecordPermission(record);
        return new AnalysisRecordDetailDto(
                record.getId(),
                record.getResumeName(),
                record.getResumeText(),
                record.getJobDescription(),
                parseTargetContext(record.getTargetContextJson()),
                record.getMatchScore(),
                record.getSummary(),
                parseResult(record),
                record.getCreatedAt());
    }

    public void deleteRecord(Long id) {
        AnalysisRecord record = recordRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("分析记录不存在"));
        checkRecordPermission(record);
        recordRepository.delete(record);
    }

    private AnalyzeResponse analyzeWithAi(AnalyzeRequest request, String effectiveJobDescription) {
        String systemPrompt = """
                你是一名资深互联网后端和 AI 应用面试官。请根据学生简历和岗位目标做求职分析。
                如果岗位目标里包含联网搜索补充，请把它视为公开岗位背景信息，结合用户填写内容一起判断。
                必须只返回 JSON，不要输出 Markdown，也不要输出解释文字。
                JSON 字段为：matchScore(number), summary(string), strengths(string[]), gaps(string[]),
                suggestions(string[]), projectRewrite(string[]), interviewQuestions(string[])。
                matchScore 取值 0 到 100。所有内容使用中文，建议要具体可执行。
                """;

        String userPrompt = """
                【学生简历】
                %s

                【岗位目标】
                %s
                """.formatted(limit(request.getResumeText(), 12000), limit(effectiveJobDescription, 9000));

        try {
            String content = aiClient.chat(systemPrompt, userPrompt, request.getAiConfig());
            return objectMapper.readValue(extractJson(content), AnalyzeResponse.class);
        } catch (Exception exception) {
            log.warn("AI analysis failed, fallback to local result: {}", exception.getMessage());
            AnalyzeResponse response = mockAnalyze();
            response.setSummary("AI 返回内容解析失败，当前展示本地兜底结果。请检查模型是否按 JSON 格式返回。");
            return response;
        }
    }

    private void saveRecord(AnalyzeRequest request, JobTargetContext targetContext, String effectiveJobDescription, AnalyzeResponse response) {
        try {
            AnalysisRecord record = new AnalysisRecord();
            authContext.currentUser().ifPresent(user -> record.setUserId(user.userId()));
            record.setResumeName(StringUtils.hasText(request.getResumeName()) ? request.getResumeName() : "未命名简历");
            record.setResumeText(limit(request.getResumeText(), 20000));
            record.setJobDescription(limit(effectiveJobDescription, 20000));
            record.setTargetContextJson(objectMapper.writeValueAsString(targetContext));
            record.setMatchScore(response.getMatchScore());
            record.setSummary(response.getSummary());
            record.setResultJson(objectMapper.writeValueAsString(response));
            recordRepository.save(record);
        } catch (Exception ignored) {
        }
    }

    private AnalyzeResponse mockAnalyze() {
        AnalyzeResponse response = new AnalyzeResponse();
        response.setMatchScore(78);
        response.setSummary("当前处于本地兜底模式：未成功使用 AI 生成结果。请检查 AI 配置、中转站连通性以及模型返回格式。");
        response.setStrengths(List.of(
                "具备 Java 后端基础，适合突出 Spring Boot 项目经历",
                "项目方向包含 AI 应用场景，展示完整工程链路有优势",
                "如果岗位目标足够具体，可以把简历亮点进一步对齐到业务场景"));
        response.setGaps(List.of(
                "需要把项目成果量化，例如接口数量、耗时优化、支持文件类型等",
                "需要更明确地区分岗位必备技能和可加分技能",
                "如果目标是某家公司岗位，建议准备更贴近业务场景的案例表达"));
        response.setSuggestions(List.of(
                "将项目经历按背景、方案、职责、结果四段书写",
                "突出后端接口、AI 调用封装、文档解析和数据持久化",
                "针对目标公司和岗位补充相关技术关键词与业务理解"));
        response.setProjectRewrite(List.of(
                "基于 Spring Boot + Vue3 实现 AI 简历分析与模拟面试平台，负责后端接口设计、简历文本解析、大模型调用封装和分析记录持久化。",
                "通过 Apache Tika 支持 PDF/DOCX/TXT 文档解析，结合岗位目标生成匹配度评分、技能短板和面试题，提升求职准备效率。"));
        response.setInterviewQuestions(List.of(
                "请介绍 CareerPilot 的整体架构和一次完整分析请求的链路。",
                "如果岗位目标来自一张截图，你会如何提取和利用其中的结构化信息？",
                "你是如何保证 AI API Key 不暴露到前端的？",
                "如果模型返回格式不稳定，你在后端如何处理？"));
        return response;
    }

    private AnalyzeResponse parseResult(AnalysisRecord record) {
        try {
            return objectMapper.readValue(record.getResultJson(), AnalyzeResponse.class);
        } catch (Exception exception) {
            AnalyzeResponse response = new AnalyzeResponse();
            response.setMatchScore(record.getMatchScore());
            response.setSummary(record.getSummary());
            return response;
        }
    }

    private JobTargetContext parseTargetContext(String json) {
        try {
            if (!StringUtils.hasText(json)) {
                return null;
            }
            return objectMapper.readValue(json, JobTargetContext.class);
        } catch (Exception exception) {
            return null;
        }
    }

    private void checkRecordPermission(AnalysisRecord record) {
        Long currentUserId = authContext.currentUser().map(TokenPayload::userId).orElse(null);
        Long recordUserId = record.getUserId();
        if (recordUserId == null && currentUserId == null) {
            return;
        }
        if (recordUserId != null && recordUserId.equals(currentUserId)) {
            return;
        }
        throw new IllegalArgumentException("无权访问该分析记录");
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

    private String limit(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
