package com.careerpilot.service;

import com.careerpilot.dto.AiRuntimeConfig;
import com.careerpilot.dto.JobTargetContext;
import com.careerpilot.dto.JobTargetResolveRequest;
import com.careerpilot.dto.WebSearchResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class JobTargetService {

    private static final Logger log = LoggerFactory.getLogger(JobTargetService.class);

    private final AiClient aiClient;
    private final ObjectMapper objectMapper;
    private final WebSearchService webSearchService;

    public JobTargetService(AiClient aiClient, ObjectMapper objectMapper, WebSearchService webSearchService) {
        this.aiClient = aiClient;
        this.objectMapper = objectMapper;
        this.webSearchService = webSearchService;
    }

    public JobTargetContext resolve(JobTargetResolveRequest request) {
        JobTargetContext context = new JobTargetContext();
        context.setCompanyName(normalize(request.getCompanyName()));
        context.setRoleTitle(normalize(request.getRoleTitle()));
        context.setDescription(limit(normalize(request.getDescription()), 6000));
        context.setImageFileName(normalize(request.getImageFileName()));

        if (StringUtils.hasText(request.getImageDataUrl()) && aiClient.isConfigured(request.getAiConfig())) {
            applyImageExtraction(context, request.getImageDataUrl(), request.getAiConfig());
        }

        if (aiClient.isConfigured(request.getAiConfig()) && shouldExtractFromText(context)) {
            applyTextExtraction(context, request.getAiConfig());
        }

        applyHeuristicExtraction(context);
        maybeSearch(context);
        return context;
    }

    public JobTargetContext enrichForPrompt(String jobDescription, JobTargetContext targetContext, AiRuntimeConfig aiConfig) {
        JobTargetResolveRequest request = new JobTargetResolveRequest();
        if (targetContext != null) {
            request.setCompanyName(targetContext.getCompanyName());
            request.setRoleTitle(targetContext.getRoleTitle());
            request.setDescription(StringUtils.hasText(targetContext.getDescription()) ? targetContext.getDescription() : jobDescription);
            request.setImageFileName(targetContext.getImageFileName());
            JobTargetContext resolved = resolveWithoutImage(request, aiConfig, targetContext);
            if ((resolved.getWebSearchResults() == null || resolved.getWebSearchResults().isEmpty())
                    && targetContext.getWebSearchResults() != null) {
                resolved.setWebSearchResults(targetContext.getWebSearchResults());
            }
            if (!StringUtils.hasText(resolved.getEnrichmentSummary())) {
                resolved.setEnrichmentSummary(targetContext.getEnrichmentSummary());
            }
            if (!StringUtils.hasText(resolved.getSearchHint())) {
                resolved.setSearchHint(targetContext.getSearchHint());
            }
            if (!resolved.isSearchPerformed()) {
                resolved.setSearchPerformed(targetContext.isSearchPerformed());
            }
            if (!StringUtils.hasText(resolved.getImageExtractedText())) {
                resolved.setImageExtractedText(targetContext.getImageExtractedText());
            }
            return resolved;
        }
        request.setDescription(jobDescription);
        return resolveWithoutImage(request, aiConfig, null);
    }

    public String buildEffectiveJobDescription(String fallbackJobDescription, JobTargetContext targetContext) {
        List<String> sections = new ArrayList<>();
        if (StringUtils.hasText(targetContext.getCompanyName()) || StringUtils.hasText(targetContext.getRoleTitle())) {
            StringBuilder header = new StringBuilder();
            if (StringUtils.hasText(targetContext.getCompanyName())) {
                header.append("目标公司：").append(targetContext.getCompanyName());
            }
            if (StringUtils.hasText(targetContext.getRoleTitle())) {
                if (header.length() > 0) {
                    header.append("\n");
                }
                header.append("目标岗位：").append(targetContext.getRoleTitle());
            }
            sections.add(header.toString());
        }
        if (StringUtils.hasText(targetContext.getDescription())) {
            sections.add("用户填写的岗位描述：\n" + targetContext.getDescription());
        } else if (StringUtils.hasText(fallbackJobDescription)) {
            sections.add("岗位描述：\n" + fallbackJobDescription.trim());
        }
        if (StringUtils.hasText(targetContext.getImageExtractedText())) {
            sections.add("岗位图片识别文本：\n" + targetContext.getImageExtractedText());
        }
        if (StringUtils.hasText(targetContext.getEnrichmentSummary())) {
            sections.add("联网搜索补充：\n" + targetContext.getEnrichmentSummary());
        }
        return String.join("\n\n", sections).trim();
    }

    public boolean hasAnyTargetInfo(String jobDescription, JobTargetContext targetContext) {
        if (StringUtils.hasText(jobDescription)) {
            return true;
        }
        if (targetContext == null) {
            return false;
        }
        return StringUtils.hasText(targetContext.getCompanyName())
                || StringUtils.hasText(targetContext.getRoleTitle())
                || StringUtils.hasText(targetContext.getDescription())
                || StringUtils.hasText(targetContext.getImageExtractedText());
    }

    private JobTargetContext resolveWithoutImage(JobTargetResolveRequest request, AiRuntimeConfig aiConfig, JobTargetContext sourceContext) {
        JobTargetContext context = new JobTargetContext();
        context.setCompanyName(normalize(request.getCompanyName()));
        context.setRoleTitle(normalize(request.getRoleTitle()));
        context.setDescription(limit(normalize(request.getDescription()), 6000));
        context.setImageFileName(normalize(request.getImageFileName()));
        if (sourceContext != null) {
            context.setImageExtractedText(limit(normalize(sourceContext.getImageExtractedText()), 6000));
            context.setSearchQuery(normalize(sourceContext.getSearchQuery()));
            context.setEnrichmentSummary(limit(normalize(sourceContext.getEnrichmentSummary()), 6000));
            context.setSearchHint(limit(normalize(sourceContext.getSearchHint()), 300));
            context.setSearchPerformed(sourceContext.isSearchPerformed());
            context.setWebSearchResults(sourceContext.getWebSearchResults());
        }
        if (aiClient.isConfigured(aiConfig) && shouldExtractFromText(context)) {
            applyTextExtraction(context, aiConfig);
        }
        applyHeuristicExtraction(context);
        if ((context.getWebSearchResults() == null || context.getWebSearchResults().isEmpty())
                && !StringUtils.hasText(context.getEnrichmentSummary())
                && !StringUtils.hasText(context.getSearchHint())) {
            maybeSearch(context);
        }
        return context;
    }

    private boolean shouldExtractFromText(JobTargetContext context) {
        return (!StringUtils.hasText(context.getCompanyName()) || !StringUtils.hasText(context.getRoleTitle()))
                && (StringUtils.hasText(context.getDescription()) || StringUtils.hasText(context.getImageExtractedText()));
    }

    private void applyImageExtraction(JobTargetContext context, String imageDataUrl, AiRuntimeConfig aiConfig) {
        String systemPrompt = """
                你是岗位信息提取助手。请读取招聘截图并只返回 JSON。
                JSON 字段固定为：companyName(string), roleTitle(string), extractedText(string)。
                如果无法识别某个字段，返回空字符串。
                """;
        List<Map<String, Object>> userContent = List.of(
                Map.of("type", "text", "text", "请识别截图中的公司名称、岗位名称，并尽量提取和岗位要求有关的文本。"),
                Map.of("type", "image_url", "image_url", Map.of("url", imageDataUrl))
        );
        try {
            String content = aiClient.chatWithUserContent(systemPrompt, userContent, aiConfig);
            TargetExtraction extraction = objectMapper.readValue(extractJson(content), TargetExtraction.class);
            applyExtraction(context, extraction);
        } catch (Exception exception) {
            log.warn("job target image extraction failed: {}", exception.getMessage());
        }
    }

    private void applyTextExtraction(JobTargetContext context, AiRuntimeConfig aiConfig) {
        String systemPrompt = """
                你是岗位信息提取助手。请从文本中提取目标公司和岗位，只返回 JSON。
                JSON 字段固定为：companyName(string), roleTitle(string), extractedText(string)。
                extractedText 用于整理与岗位要求相关的关键信息摘要。
                """;
        String userPrompt = """
                请从下面内容中提取岗位信息。如果没有对应字段就返回空字符串。

                【岗位描述】
                %s

                【岗位图片识别文本】
                %s
                """.formatted(defaultValue(context.getDescription()), defaultValue(context.getImageExtractedText()));
        try {
            String content = aiClient.chat(systemPrompt, userPrompt, aiConfig);
            TargetExtraction extraction = objectMapper.readValue(extractJson(content), TargetExtraction.class);
            applyExtraction(context, extraction);
        } catch (Exception exception) {
            log.warn("job target text extraction failed: {}", exception.getMessage());
        }
    }

    private void applyExtraction(JobTargetContext context, TargetExtraction extraction) {
        if (extraction == null) {
            return;
        }
        if (!StringUtils.hasText(context.getCompanyName())) {
            context.setCompanyName(normalize(extraction.getCompanyName()));
        }
        if (!StringUtils.hasText(context.getRoleTitle())) {
            context.setRoleTitle(normalize(extraction.getRoleTitle()));
        }
        if (!StringUtils.hasText(context.getImageExtractedText()) && StringUtils.hasText(extraction.getExtractedText())) {
            context.setImageExtractedText(limit(normalize(extraction.getExtractedText()), 6000));
        }
    }

    private void applyHeuristicExtraction(JobTargetContext context) {
        if (!StringUtils.hasText(context.getCompanyName())) {
            context.setCompanyName(findLabeledValue(context, List.of("公司", "公司名称", "企业", "用人单位")));
        }
        if (!StringUtils.hasText(context.getRoleTitle())) {
            context.setRoleTitle(findLabeledValue(context, List.of("岗位", "岗位名称", "职位", "目标岗位", "招聘岗位")));
        }
    }

    private String findLabeledValue(JobTargetContext context, List<String> labels) {
        String combined = String.join("\n", defaultValue(context.getDescription()), defaultValue(context.getImageExtractedText()));
        for (String line : combined.split("\\r?\\n")) {
            String trimmed = line.trim();
            for (String label : labels) {
                if (trimmed.startsWith(label + "：") || trimmed.startsWith(label + ":")) {
                    return normalize(trimmed.substring(label.length() + 1));
                }
            }
        }
        return "";
    }

    private void maybeSearch(JobTargetContext context) {
        if (!StringUtils.hasText(context.getCompanyName()) || !StringUtils.hasText(context.getRoleTitle())) {
            return;
        }
        List<WebSearchResult> rawResults = webSearchService.searchCompanyRole(context.getCompanyName(), context.getRoleTitle());
        List<ScoredSearchResult> scoredResults = scoreSearchResults(context, rawResults);
        List<WebSearchResult> results = filterSearchResults(context, scoredResults);
        context.setSearchQuery(context.getCompanyName() + " " + context.getRoleTitle());
        context.setWebSearchResults(results);
        if (results.isEmpty()) {
            context.setSearchPerformed(false);
            context.setEnrichmentSummary("");
            context.setSearchHint(buildSearchHint(context, findCareerPortalResults(context, scoredResults)));
            return;
        }
        context.setSearchPerformed(true);
        context.setSearchHint("");
        context.setEnrichmentSummary(formatSearchResults(results));
    }

    private String formatSearchResults(List<WebSearchResult> results) {
        if (results == null || results.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        int limit = Math.min(results.size(), 5);
        for (int index = 0; index < limit; index++) {
            WebSearchResult result = results.get(index);
            if (builder.length() > 0) {
                builder.append("\n\n");
            }
            builder.append(index + 1)
                    .append(". ")
                    .append(defaultValue(result.getTitle()));
            if (StringUtils.hasText(result.getSnippet())) {
                builder.append("\n").append(result.getSnippet());
            }
            if (StringUtils.hasText(result.getLink())) {
                builder.append("\n来源：").append(result.getLink());
            }
        }
        return builder.toString();
    }

    private List<ScoredSearchResult> scoreSearchResults(JobTargetContext context, List<WebSearchResult> rawResults) {
        if (rawResults == null || rawResults.isEmpty()) {
            return List.of();
        }

        return rawResults.stream()
                .map(result -> new ScoredSearchResult(result, scoreSearchResult(context, result)))
                .sorted(Comparator.comparingInt(ScoredSearchResult::score).reversed())
                .toList();
    }

    private List<WebSearchResult> filterSearchResults(JobTargetContext context, List<ScoredSearchResult> scoredResults) {
        if (scoredResults == null || scoredResults.isEmpty()) {
            return List.of();
        }

        return scoredResults.stream()
                .filter(item -> isHighConfidenceJobRequirement(context, item.result(), item.score()))
                .limit(5)
                .map(ScoredSearchResult::result)
                .toList();
    }

    private int scoreSearchResult(JobTargetContext context, WebSearchResult result) {
        String title = lower(defaultValue(result.getTitle()));
        String snippet = lower(defaultValue(result.getSnippet()));
        String link = lower(defaultValue(result.getLink()));
        String text = title + "\n" + snippet + "\n" + link;

        int score = 0;
        Set<String> companyKeywords = extractCompanyKeywords(context);
        String roleTitle = lower(context.getRoleTitle());

        if (containsAnyKeyword(title, companyKeywords)) {
            score += 30;
        } else if (containsAnyKeyword(text, companyKeywords)) {
            score += 18;
        }

        if (containsText(title, roleTitle)) {
            score += 34;
        } else if (containsText(text, roleTitle)) {
            score += 22;
        }

        for (String roleHint : extractRoleHintSignals(context)) {
            if (title.contains(roleHint)) {
                score += 10;
            } else if (text.contains(roleHint)) {
                score += 5;
            }
        }

        for (String keyword : extractRoleKeywords(context)) {
            if (keyword.length() < 2) {
                continue;
            }
            if (title.contains(keyword)) {
                score += 8;
            } else if (text.contains(keyword)) {
                score += 4;
            }
        }

        for (String hiringSignal : hiringSignals()) {
            if (title.contains(hiringSignal)) {
                score += 10;
            } else if (text.contains(hiringSignal)) {
                score += 5;
            }
        }

        for (String requirementSignal : requirementSignals()) {
            if (title.contains(requirementSignal)) {
                score += 12;
            } else if (snippet.contains(requirementSignal)) {
                score += 8;
            } else if (text.contains(requirementSignal)) {
                score += 4;
            }
        }

        for (String officialSignal : officialSignals()) {
            if (link.contains(officialSignal)) {
                score += 8;
            }
        }

        for (String platformSignal : jobPlatformSignals()) {
            if (link.contains(platformSignal)) {
                score += 12;
            } else if (title.contains(platformSignal)) {
                score += 8;
            } else if (snippet.contains(platformSignal)) {
                score += 4;
            }
        }

        for (String genericSignal : genericCompanySignals()) {
            if (title.contains(genericSignal) || snippet.contains(genericSignal)) {
                score += 2;
            }
        }

        for (String penaltySignal : weakSignals()) {
            if (title.contains(penaltySignal) || link.contains(penaltySignal)) {
                score -= 14;
            } else if (snippet.contains(penaltySignal)) {
                score -= 8;
            }
        }

        if (!containsAny(text, hiringSignals()) && containsAny(text, weakSignals())) {
            score -= 12;
        }

        if (link.contains("github.com")) {
            score -= 20;
        }
        if (link.contains("/products") || link.contains("/product")) {
            score -= 16;
        }
        if (!containsAny(text, hiringSignals())
                && !containsAny(text, List.copyOf(extractRoleKeywords(context)))
                && !containsAny(text, List.copyOf(extractRoleHintSignals(context)))) {
            score -= 10;
        }

        return score;
    }

    private boolean isHighConfidenceJobRequirement(JobTargetContext context, WebSearchResult result, int score) {
        if (score < 24) {
            return false;
        }

        String title = lower(defaultValue(result.getTitle()));
        String snippet = lower(defaultValue(result.getSnippet()));
        String link = lower(defaultValue(result.getLink()));
        String text = title + "\n" + snippet + "\n" + link;

        Set<String> companyKeywords = extractCompanyKeywords(context);
        String roleTitle = lower(context.getRoleTitle());
        Set<String> roleKeywords = extractRoleKeywords(context);
        Set<String> roleHints = extractRoleHintSignals(context);

        boolean companyMatched = containsAnyKeyword(text, companyKeywords);
        boolean roleMatched = containsText(text, roleTitle);
        int roleKeywordHits = countMatches(text, roleKeywords);
        boolean roleHintMatched = countMatches(text, roleHints) > 0;
        boolean hiringSignalMatched = containsAny(text, hiringSignals());
        boolean requirementSignalMatched = containsAny(text, requirementSignals());
        boolean platformSignalMatched = containsAny(text, jobPlatformSignals());
        boolean dictionarySignalMatched = containsAny(text, dictionarySignals());
        boolean weakSignalMatched = containsAny(text, weakSignals());

        if (!companyMatched) {
            return false;
        }
        if (!roleMatched && roleKeywordHits < 1 && !roleHintMatched) {
            return false;
        }
        if (!hiringSignalMatched) {
            return false;
        }
        if (!requirementSignalMatched && !platformSignalMatched) {
            return false;
        }
        if (dictionarySignalMatched) {
            return false;
        }

        if (weakSignalMatched && !platformSignalMatched && !requirementSignalMatched) {
            return false;
        }

        return requirementSignalMatched || platformSignalMatched || roleHintMatched || score >= 36;
    }

    private List<WebSearchResult> findCareerPortalResults(JobTargetContext context, List<ScoredSearchResult> scoredResults) {
        if (scoredResults == null || scoredResults.isEmpty()) {
            return List.of();
        }

        return scoredResults.stream()
                .filter(item -> isCareerPortalCandidate(context, item.result(), item.score()))
                .limit(2)
                .map(ScoredSearchResult::result)
                .toList();
    }

    private boolean isCareerPortalCandidate(JobTargetContext context, WebSearchResult result, int score) {
        if (score < 12) {
            return false;
        }

        String title = lower(defaultValue(result.getTitle()));
        String snippet = lower(defaultValue(result.getSnippet()));
        String link = lower(defaultValue(result.getLink()));
        String text = title + "\n" + snippet + "\n" + link;

        boolean companyMatched = containsAnyKeyword(text, extractCompanyKeywords(context));
        boolean hiringSignalMatched = containsAny(text, hiringSignals());
        boolean officialSignalMatched = containsAny(link, officialSignals()) || containsAny(link, jobPlatformSignals());
        boolean dictionarySignalMatched = containsAny(text, dictionarySignals());
        boolean weakSignalMatched = containsAny(text, weakSignals());

        if (!companyMatched) {
            return false;
        }
        if (!hiringSignalMatched && !officialSignalMatched) {
            return false;
        }
        if (dictionarySignalMatched) {
            return false;
        }

        return !weakSignalMatched || officialSignalMatched;
    }

    private Set<String> extractRoleKeywords(JobTargetContext context) {
        LinkedHashSet<String> keywords = new LinkedHashSet<>();
        addKeywords(keywords, context.getRoleTitle());
        addKeywords(keywords, context.getDescription());
        addKeywords(keywords, context.getImageExtractedText());
        keywords.removeIf(keyword -> keyword.length() < 2 || stopKeywords().contains(keyword));
        return keywords;
    }

    private Set<String> extractCompanyKeywords(JobTargetContext context) {
        LinkedHashSet<String> keywords = new LinkedHashSet<>();
        String companyName = lower(context.getCompanyName());
        if (!StringUtils.hasText(companyName)) {
            return keywords;
        }

        keywords.add(companyName);
        if (companyName.contains("\u963f\u91cc") || companyName.contains("alibaba")) {
            keywords.add("\u963f\u91cc");
            keywords.add("\u6dd8\u5b9d");
            keywords.add("\u5929\u732b");
            keywords.add("\u6dd8\u5929");
            keywords.add("\u963f\u91cc\u4e91");
            keywords.add("\u76d2\u9a6c");
            keywords.add("\u83dc\u9e1f");
            keywords.add("\u9ad8\u5fb7");
            keywords.add("\u95f2\u9c7c");
            keywords.add("\u5927\u6587\u5a31");
        }
        if (companyName.contains("\u817e\u8baf") || companyName.contains("tencent")) {
            keywords.add("\u817e\u8baf");
            keywords.add("\u5fae\u4fe1");
            keywords.add("qq");
            keywords.add("\u817e\u8baf\u4e91");
            keywords.add("\u817e\u8baf\u4e91\u667a");
        }
        if (companyName.contains("\u5b57\u8282") || companyName.contains("bytedance")) {
            keywords.add("\u5b57\u8282");
            keywords.add("\u6296\u97f3");
            keywords.add("tiktok");
            keywords.add("\u4eca\u65e5\u5934\u6761");
        }
        return keywords;
    }

    private Set<String> extractRoleHintSignals(JobTargetContext context) {
        LinkedHashSet<String> signals = new LinkedHashSet<>();
        String roleTitle = lower(context.getRoleTitle());
        if (!StringUtils.hasText(roleTitle)) {
            return signals;
        }

        if (roleTitle.contains("java")) {
            signals.add("java");
        }
        if (roleTitle.contains("后端") || roleTitle.contains("backend")) {
            signals.add("后端");
            signals.add("后台");
            signals.add("开发");
            signals.add("backend");
            signals.add("server");
            signals.add("服务端");
        }
        if (roleTitle.contains("前端") || roleTitle.contains("frontend")) {
            signals.add("前端");
            signals.add("web");
            signals.add("frontend");
        }
        if (roleTitle.contains("产品")) {
            signals.add("产品");
            signals.add("产品经理");
        }
        if (roleTitle.contains("数据")) {
            signals.add("数据");
            signals.add("分析");
        }
        if (roleTitle.contains("运营")) {
            signals.add("运营");
        }
        if (roleTitle.contains("算法")) {
            signals.add("算法");
            signals.add("推荐");
            signals.add("机器学习");
        }
        if (roleTitle.contains("测试")) {
            signals.add("测试");
            signals.add("qa");
        }
        return signals;
    }

    private void addKeywords(Set<String> keywords, String text) {
        if (!StringUtils.hasText(text)) {
            return;
        }

        String normalized = lower(text)
                .replaceAll("[\\p{Punct}\\p{IsPunctuation}]+", " ")
                .replaceAll("\\s+", " ")
                .trim();

        for (String part : normalized.split(" ")) {
            String token = part.trim();
            if (token.length() >= 2) {
                keywords.add(token);
            }
        }

        for (String chineseToken : normalized.split("[a-z0-9\\s]+")) {
            String token = chineseToken.trim();
            if (token.length() >= 2) {
                keywords.add(token);
                addChineseSubKeywords(keywords, token);
            }
        }
    }

    private void addChineseSubKeywords(Set<String> keywords, String token) {
        if (!StringUtils.hasText(token) || token.length() < 2) {
            return;
        }
        int maxWindow = Math.min(token.length(), 4);
        for (int window = 2; window <= maxWindow; window++) {
            for (int index = 0; index + window <= token.length(); index++) {
                String part = token.substring(index, index + window).trim();
                if (part.length() >= 2) {
                    keywords.add(part);
                }
            }
        }
    }

    private int countMatches(String text, Set<String> keywords) {
        int matches = 0;
        for (String keyword : keywords) {
            if (keyword.length() >= 2 && text.contains(keyword)) {
                matches++;
            }
        }
        return matches;
    }

    private boolean containsAny(String text, List<String> signals) {
        for (String signal : signals) {
            if (text.contains(signal)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsAnyKeyword(String text, Set<String> keywords) {
        for (String keyword : keywords) {
            if (keyword.length() >= 2 && text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private boolean containsText(String text, String needle) {
        return StringUtils.hasText(needle) && text.contains(needle);
    }

    private String lower(String value) {
        return defaultValue(value).toLowerCase(Locale.ROOT);
    }

    private List<String> hiringSignals() {
        return List.of(
                "job", "jobs", "career", "careers", "hiring", "join", "intern", "internship",
                "\u62db\u8058", "\u5c97\u4f4d", "\u804c\u4f4d", "\u5b9e\u4e60", "\u6821\u62db",
                "\u793e\u62db", "jd", "\u804c\u8d23", "\u8981\u6c42", "\u4efb\u804c"
        );
    }

    private List<String> officialSignals() {
        return List.of("career", "careers", "jobs", "join", "/job", "/careers", "/campus");
    }

    private List<String> requirementSignals() {
        return List.of(
                "responsibilit", "requirement", "qualification", "what you will do", "what we're looking for",
                "\u5c97\u4f4d\u804c\u8d23", "\u5c97\u4f4d\u8981\u6c42", "\u4efb\u804c\u8981\u6c42",
                "\u804c\u4f4d\u63cf\u8ff0", "\u5de5\u4f5c\u5185\u5bb9", "\u5de5\u4f5c\u804c\u8d23",
                "\u804c\u8d23", "\u8981\u6c42", "\u4efb\u804c", "\u8d44\u683c"
        );
    }

    private List<String> jobPlatformSignals() {
        return List.of(
                "zhipin", "boss", "liepin", "lagou", "51job", "jobui", "kanzhun", "yingjiesheng", "shixiseng",
                "\u76f4\u8058", "\u5b9e\u4e60\u50e7", "\u725b\u5ba2", "\u730e\u8058", "\u62c9\u52fe", "\u524d\u7a0b\u65e0\u5fe7",
                "jobs", "careers", "/job", "/jobs", "/careers", "/position"
        );
    }

    private List<String> genericCompanySignals() {
        return List.of(
                "\u5e73\u53f0", "\u4e1a\u52a1", "\u4ea7\u54c1", "\u6280\u672f", "\u7814\u53d1", "engineering"
        );
    }

    private List<String> weakSignals() {
        return List.of(
                "wikipedia", "wiki", "baike", "zhihu", "stock", "finance", "quote", "video",
                "\u767e\u79d1", "\u80a1\u7968", "\u8d22\u7ecf", "\u884c\u60c5", "\u7ef4\u57fa", "\u89c6\u9891"
        );
    }

    private List<String> dictionarySignals() {
        return List.of(
                "meaning", "definition", "pronunciation", "dictionary",
                "\u610f\u601d", "\u89e3\u91ca", "\u542b\u4e49", "\u8bfb\u97f3", "\u62fc\u97f3", "\u600e\u4e48\u8bfb"
        );
    }

    private Set<String> stopKeywords() {
        return Set.of(
                "intern", "\u5b9e\u4e60", "\u6821\u62db", "\u6691\u671f",
                "\u516c\u53f8", "\u5c97\u4f4d", "\u804c\u4f4d", "\u62db\u8058", "\u8981\u6c42",
                "\u63cf\u8ff0", "\u76ee\u6807"
        );
    }

    private String extractJson(String content) {
        if (!StringUtils.hasText(content)) {
            return "{}";
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

    private String defaultValue(String value) {
        return value == null ? "" : value;
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.replaceAll("\\s+", " ").trim();
    }

    private String limit(String value, int maxLength) {
        if (!StringUtils.hasText(value) || value.length() <= maxLength) {
            return value == null ? "" : value;
        }
        return value.substring(0, maxLength);
    }

    private String buildSearchHint(JobTargetContext context, List<WebSearchResult> fallbackResults) {
        if (fallbackResults != null && !fallbackResults.isEmpty()) {
            StringBuilder builder = new StringBuilder();
            builder.append("\u53ea\u5b9a\u4f4d\u5230\u201c")
                    .append(context.getCompanyName())
                    .append(" ")
                    .append(context.getRoleTitle())
                    .append("\u201d\u7684\u62db\u8058\u5165\u53e3\uff0c\u4f46\u8fd8\u6ca1\u627e\u5230\u660e\u786e\u7684\u5c97\u4f4d\u8981\u6c42\uff1a");
            for (int index = 0; index < fallbackResults.size(); index++) {
                WebSearchResult result = fallbackResults.get(index);
                builder.append("\n")
                        .append(index + 1)
                        .append(". ")
                        .append(defaultValue(result.getTitle()));
                if (StringUtils.hasText(result.getLink())) {
                    builder.append("\n").append(result.getLink());
                }
            }
            builder.append("\n\u5efa\u8bae\u7c98\u8d34\u66f4\u5b8c\u6574\u7684 JD \u6216\u4e0a\u4f20\u5c97\u4f4d\u622a\u56fe\uff0c\u8fd9\u6837\u6211\u4eec\u624d\u80fd\u56f4\u7ed5\u8fd9\u4e2a\u5c97\u4f4d\u7ed9\u51fa\u66f4\u51c6\u7684\u5206\u6790\u548c\u9762\u8bd5\u9898\u3002");
            return builder.toString();
        }

        return "\u672a\u68c0\u7d22\u5230\u9ad8\u76f8\u5173\u7684\u201c"
                + context.getCompanyName()
                + " "
                + context.getRoleTitle()
                + "\u201d\u5c97\u4f4d\u62db\u8058\u9700\u6c42\uff0c\u5efa\u8bae\u7c98\u8d34\u66f4\u5b8c\u6574\u7684 JD \u6216\u4e0a\u4f20\u5c97\u4f4d\u622a\u56fe\u3002";
    }

    private static class TargetExtraction {
        private String companyName;
        private String roleTitle;
        private String extractedText;

        public String getCompanyName() {
            return companyName;
        }

        public void setCompanyName(String companyName) {
            this.companyName = companyName;
        }

        public String getRoleTitle() {
            return roleTitle;
        }

        public void setRoleTitle(String roleTitle) {
            this.roleTitle = roleTitle;
        }

        public String getExtractedText() {
            return extractedText;
        }

        public void setExtractedText(String extractedText) {
            this.extractedText = extractedText;
        }
    }

    private record ScoredSearchResult(WebSearchResult result, int score) {
    }
}
