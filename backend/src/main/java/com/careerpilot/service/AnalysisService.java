package com.careerpilot.service;

import com.careerpilot.dto.AnalysisRecordDto;
import com.careerpilot.dto.AnalysisRecordDetailDto;
import com.careerpilot.dto.AnalyzeRequest;
import com.careerpilot.dto.AnalyzeResponse;
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

    public AnalysisService(
            AiClient aiClient,
            ObjectMapper objectMapper,
            AnalysisRecordRepository recordRepository,
            AuthContext authContext) {
        this.aiClient = aiClient;
        this.objectMapper = objectMapper;
        this.recordRepository = recordRepository;
        this.authContext = authContext;
    }

    public AnalyzeResponse analyze(AnalyzeRequest request) {
        AnalyzeResponse response = aiClient.isConfigured(request.getAiConfig())
                ? analyzeWithAi(request)
                : mockAnalyze();
        saveRecord(request, response);
        return response;
    }

    public List<AnalysisRecordDto> latestRecords() {
        List<AnalysisRecord> records = authContext.currentUser()
                .map(user -> recordRepository.findTop10ByUserIdOrderByCreatedAtDesc(user.userId()))
                .orElseGet(recordRepository::findTop10ByUserIdIsNullOrderByCreatedAtDesc);
        return records
                .stream()
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

    private AnalyzeResponse analyzeWithAi(AnalyzeRequest request) {
        String systemPrompt = """
                你是一名资深互联网后端和 AI 应用面试官。请根据学生简历和岗位 JD 做求职分析。
                必须只返回 JSON，不要输出 Markdown，不要输出解释文字。
                JSON 字段为：matchScore(number), summary(string), strengths(string[]), gaps(string[]),
                suggestions(string[]), projectRewrite(string[]), interviewQuestions(string[])。
                matchScore 取 0 到 100。所有内容使用中文，建议要具体可执行。
                """;

        String userPrompt = """
                【学生简历】
                %s

                【岗位 JD】
                %s
                """.formatted(limit(request.getResumeText(), 12000), limit(request.getJobDescription(), 6000));

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

    private void saveRecord(AnalyzeRequest request, AnalyzeResponse response) {
        try {
            AnalysisRecord record = new AnalysisRecord();
            authContext.currentUser().ifPresent(user -> record.setUserId(user.userId()));
            record.setResumeName(StringUtils.hasText(request.getResumeName()) ? request.getResumeName() : "未命名简历");
            record.setResumeText(limit(request.getResumeText(), 20000));
            record.setJobDescription(limit(request.getJobDescription(), 20000));
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
        response.setSummary("本地兜底模式：当前未成功使用 AI 生成结果。请检查后端是否读取到 .env、模型名是否可用，以及中转站是否有调用记录。");
        response.setStrengths(List.of("具备 Java 后端基础，适合包装 Spring Boot 项目经历", "项目方向包含 AI 应用场景，招聘会展示效果较直观", "可以通过简历解析、模型调用和历史记录体现完整工程链路"));
        response.setGaps(List.of("需要补充数据库设计、接口性能和异常处理细节", "需要把项目成果量化，例如接口数量、响应时间、支持文件类型", "需要准备一段清晰的项目架构讲解"));
        response.setSuggestions(List.of("将项目经历按背景、方案、职责、结果四段书写", "突出后端接口、AI 调用封装、文档解析和数据持久化", "补充 GitHub README、运行截图和演示数据"));
        response.setProjectRewrite(List.of("基于 Spring Boot + Vue3 实现 AI 简历分析与模拟面试平台，负责后端接口设计、简历文本解析、大模型调用封装和分析记录持久化。", "通过 Apache Tika 支持 PDF/DOCX/TXT 文档解析，结合岗位 JD 生成匹配度评分、技能短板和面试题，提升求职准备效率。"));
        response.setInterviewQuestions(List.of("请介绍 CareerPilot 的整体架构和一次完整分析请求的链路。", "你是如何保证 AI API Key 不暴露到前端的？", "如果模型返回格式不稳定，你在后端如何处理？", "H2 数据库后续切换 MySQL 需要改哪些配置？"));
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
