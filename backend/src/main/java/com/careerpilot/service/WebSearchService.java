package com.careerpilot.service;

import com.careerpilot.dto.WebSearchResult;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.util.HtmlUtils;

@Service
public class WebSearchService {

    private static final Logger log = LoggerFactory.getLogger(WebSearchService.class);

    private static final Pattern RSS_ITEM_PATTERN = Pattern.compile(
            "<item>\\s*<title>(.*?)</title>\\s*<link>(.*?)</link>\\s*<description>(.*?)</description>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern HTML_RESULT_PATTERN = Pattern.compile(
            "<li class=\"b_algo\".*?<h2[^>]*><a[^>]*href=\"(.*?)\"[^>]*>(.*?)</a></h2>.*?(?:<div class=\"b_caption\"><p[^>]*>(.*?)</p></div>)?",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern SOGOU_RESULT_PATTERN = Pattern.compile(
            "<div class=\"vrwrap\".*?<h3 class=\"vr-title\">.*?<a[^>]*href=\"(.*?)\"[^>]*>(.*?)</a></h3>.*?<div class=\"fz-mid[^\"]*\"[^>]*>(.*?)</div>.*?<a class=\"citeLinkClass\"[^>]*>.*?<span>(.*?)</span>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern SHIXISENG_SEARCH_CARD_PATTERN = Pattern.compile(
            "<div[^>]+data-intern-id=\"(inn_[^\"]+)\"[\\s\\S]*?<a href=\"(https://www\\.shixiseng\\.com/intern/[^\"]+)\" title=\"([^\"]*)\"[^>]*class=\"title[^\"]*\"[\\s\\S]*?<a title=\"([^\"]+)\" href=\"javascript:;\" class=\"title ellipsis\"",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);

    private static final Pattern SCRIPT_REDIRECT_PATTERN = Pattern.compile(
            "window\\.location(?:\\.replace)?\\(\"(.*?)\"\\)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern META_REFRESH_PATTERN = Pattern.compile(
            "URL='(.*?)'",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern TITLE_PATTERN = Pattern.compile("<title>(.*?)</title>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern META_DESCRIPTION_PATTERN = Pattern.compile(
            "<meta[^>]+name=\"description\"[^>]+content=\"(.*?)\"",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern OG_TITLE_PATTERN = Pattern.compile(
            "<meta[^>]+property=\"og:title\"[^>]+content=\"(.*?)\"",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern NOWCODER_REQUIREMENTS_PATTERN = Pattern.compile(
            "\\\\\"requirements\\\\\":\\\\\"(.*?)\\\\\",\\\\\"infos\\\\\":\\\\\"(.*?)\\\\\"",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern SHIXISENG_INFO_PATTERN = Pattern.compile(
            "p\\.info=\"(.*?)\";",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
    private static final Pattern BLOCK_TAG_PATTERN = Pattern.compile("(?i)<br\\s*/?>|</p>|</div>|</li>|</section>|</article>|</h[1-6]>");
    private static final Pattern SCRIPT_STYLE_PATTERN = Pattern.compile("(?is)<script.*?</script>|<style.*?</style>|<noscript.*?</noscript>");
    private static final Pattern TAG_PATTERN = Pattern.compile("<[^>]+>");
    private static final Pattern PRIVATE_USE_CHAR_PATTERN = Pattern.compile("[\\uE000-\\uF8FF]");
    private static final Duration NODE_TIMEOUT = Duration.ofSeconds(45);
    private static final Duration LANDING_PAGE_TIMEOUT = Duration.ofSeconds(8);
    private static final String BYTE_DANCE_POSITION_URL = "https://jobs.bytedance.com/experienced/position";
    private static final String TENCENT_QUERY_URL = "https://careers.tencent.com/tencentcareer/api/post/Query";
    private static final String TENCENT_DETAIL_URL = "https://careers.tencent.com/tencentcareer/api/post/ByPostId";
    private static final List<String> PLATFORM_QUERIES = List.of(
            "boss\u76f4\u8058",
            "\u725b\u5ba2",
            "\u725b\u5ba2\u7f51",
            "\u730e\u8058",
            "\u62c9\u52fe",
            "51job",
            "\u5b9e\u4e60\u50e7",
            "\u5e94\u5c4a\u751f",
            "\u770b\u51c6\u7f51");
    private static final List<String> PLATFORM_SITE_QUERIES = List.of(
            "site:zhipin.com",
            "site:nowcoder.com/jobs/detail",
            "site:nowcoder.com/jobs",
            "site:liepin.com",
            "site:lagou.com",
            "site:51job.com",
            "site:shixiseng.com",
            "site:yingjiesheng.com",
            "site:kanzhun.com");

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .followRedirects(HttpClient.Redirect.NORMAL)
            .build();

    private final ObjectMapper objectMapper;

    public WebSearchService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<WebSearchResult> searchCompanyRole(String companyName, String roleTitle) {
        Map<String, WebSearchResult> merged = new LinkedHashMap<>();
        mergeResults(merged, searchOfficialSources(companyName, roleTitle), 8);
        mergeResults(merged, searchPlatformSources(companyName, roleTitle), 16);

        for (String query : buildSearchQueries(companyName, roleTitle)) {
            mergeResults(merged, fetchRssResults(query), 12);
            mergeResults(merged, fetchHtmlResults(query), 24);
            mergeResults(merged, fetchSogouResults(query), 24);
            if (merged.size() >= 24) {
                break;
            }
        }
        return enrichLandingPages(new ArrayList<>(merged.values()));
    }

    private List<String> buildSearchQueries(String companyName, String roleTitle) {
        Set<String> queries = new LinkedHashSet<>();
        addQuery(queries, companyName + " " + roleTitle + " " + "\u62db\u8058");
        addQuery(queries, companyName + " " + roleTitle + " " + "\u5c97\u4f4d\u804c\u8d23");
        addQuery(queries, companyName + " " + roleTitle + " " + "\u4efb\u804c\u8981\u6c42");
        addQuery(queries, companyName + " " + roleTitle + " " + "\u4efb\u804c\u8d44\u683c");
        addQuery(queries, companyName + " " + roleTitle + " JD");
        addQuery(queries, companyName + " " + roleTitle + " jobs");
        addQuery(queries, companyName + " " + roleTitle + " careers");

        for (String platformQuery : PLATFORM_QUERIES) {
            addQuery(queries, companyName + " " + roleTitle + " " + platformQuery);
        }
        for (String siteQuery : PLATFORM_SITE_QUERIES) {
            addQuery(queries, companyName + " " + roleTitle + " " + siteQuery);
        }

        if (looksLikeInternRole(roleTitle)) {
            addQuery(queries, companyName + " " + roleTitle + " " + "\u5b9e\u4e60\u50e7");
            addQuery(queries, companyName + " " + roleTitle + " site:shixiseng.com");
        }

        return new ArrayList<>(queries);
    }

    private void addQuery(Set<String> queries, String query) {
        String normalized = normalize(query);
        if (StringUtils.hasText(normalized)) {
            queries.add(normalized);
        }
    }

    private boolean looksLikeInternRole(String roleTitle) {
        String normalized = lower(roleTitle);
        return normalized.contains("\u5b9e\u4e60")
                || normalized.contains("intern")
                || normalized.contains("internship");
    }

    private boolean looksLikeJuniorRole(String roleTitle) {
        String normalized = lower(roleTitle);
        return looksLikeInternRole(roleTitle)
                || normalized.contains("\u6821\u62db")
                || normalized.contains("\u5e94\u5c4a")
                || normalized.contains("\u6691\u671f")
                || normalized.contains("\u7ba1\u57f9");
    }

    private void mergeResults(Map<String, WebSearchResult> merged, List<WebSearchResult> results, int limit) {
        for (WebSearchResult result : results) {
            if (StringUtils.hasText(result.getLink()) && !merged.containsKey(result.getLink())) {
                merged.put(result.getLink(), result);
            }
            if (merged.size() >= limit) {
                return;
            }
        }
    }

    private List<WebSearchResult> searchOfficialSources(String companyName, String roleTitle) {
        if (!StringUtils.hasText(companyName) || !StringUtils.hasText(roleTitle)) {
            return List.of();
        }
        if (isTencentCompany(companyName)) {
            return searchTencentJobs(roleTitle);
        }
        if (isByteDanceCompany(companyName)) {
            return searchByteDanceJobs(roleTitle);
        }
        return List.of();
    }

    private List<WebSearchResult> searchPlatformSources(String companyName, String roleTitle) {
        List<WebSearchResult> results = new ArrayList<>();
        if (looksLikeJuniorRole(roleTitle)) {
            results.addAll(searchShixiseng(companyName, roleTitle));
        }
        return results;
    }

    private boolean isByteDanceCompany(String companyName) {
        String normalized = lower(companyName);
        return normalized.contains("\u5b57\u8282")
                || normalized.contains("bytedance")
                || normalized.contains("\u6296\u97f3")
                || normalized.contains("toutiao")
                || normalized.contains("tik tok")
                || normalized.contains("tiktok");
    }

    private boolean isTencentCompany(String companyName) {
        String normalized = lower(companyName);
        return normalized.contains("\u817e\u8baf")
                || normalized.contains("tencent");
    }

    private List<WebSearchResult> searchTencentJobs(String roleTitle) {
        Map<String, WebSearchResult> merged = new LinkedHashMap<>();
        for (String keyword : buildTencentRoleQueries(roleTitle)) {
            mergeResults(merged, fetchTencentJobResults(keyword), 8);
            if (merged.size() >= 6) {
                break;
            }
        }
        return new ArrayList<>(merged.values());
    }

    private List<String> buildTencentRoleQueries(String roleTitle) {
        Set<String> queries = new LinkedHashSet<>();
        addQuery(queries, roleTitle);

        String normalized = normalize(roleTitle)
                .replaceAll("(?i)internship|intern", " ")
                .replaceAll("202\\d\u5c4a", " ")
                .replaceAll("\u5b9e\u4e60|\u6691\u671f|\u6821\u62db|\u5e94\u5c4a", " ")
                .replaceAll("\\s+", " ")
                .trim();
        addQuery(queries, normalized);

        String lowered = lower(roleTitle);
        if (lowered.contains("\u540e\u7aef") || lowered.contains("backend")) {
            addQuery(queries, "\u540e\u53f0\u5f00\u53d1");
            addQuery(queries, "java \u540e\u53f0\u5f00\u53d1");
        }
        if (lowered.contains("\u4ea7\u54c1")) {
            addQuery(queries, "\u4ea7\u54c1\u7ecf\u7406");
        }
        return new ArrayList<>(queries);
    }

    private List<WebSearchResult> fetchTencentJobResults(String keyword) {
        if (!StringUtils.hasText(keyword)) {
            return List.of();
        }

        try {
            String url = TENCENT_QUERY_URL
                    + "?timestamp=" + System.currentTimeMillis()
                    + "&countryId=&cityId=&bgIds=&productId=&categoryId=&parentCategoryId=&attrId="
                    + "&keyword=" + encode(keyword)
                    + "&pageIndex=1&pageSize=10&language=zh-cn&area=cn";
            HttpResponse<byte[]> response = sendRequest(url);
            JsonNode posts = objectMapper.readTree(decodeBody(response)).path("Data").path("Posts");
            if (!posts.isArray()) {
                return List.of();
            }

            List<WebSearchResult> results = new ArrayList<>();
            for (JsonNode post : posts) {
                WebSearchResult result = fetchTencentJobDetail(post.path("PostId").asText(), post);
                if (result != null && StringUtils.hasText(result.getTitle()) && StringUtils.hasText(result.getLink())) {
                    results.add(result);
                }
                if (results.size() >= 6) {
                    break;
                }
            }
            return results;
        } catch (Exception exception) {
            log.warn("Tencent official job search failed: {}", exception.getMessage());
            return List.of();
        }
    }

    private WebSearchResult fetchTencentJobDetail(String postId, JsonNode fallbackPost) {
        if (!StringUtils.hasText(postId)) {
            return null;
        }

        try {
            String url = TENCENT_DETAIL_URL
                    + "?timestamp=" + System.currentTimeMillis()
                    + "&postId=" + encode(postId)
                    + "&language=zh-cn";
            HttpResponse<byte[]> response = sendRequest(url);
            JsonNode detail = objectMapper.readTree(decodeBody(response)).path("Data");
            if (detail.isMissingNode() || detail.isNull()) {
                detail = fallbackPost;
            }
            return toTencentWebSearchResult(detail);
        } catch (Exception ignored) {
            return toTencentWebSearchResult(fallbackPost);
        }
    }

    private WebSearchResult toTencentWebSearchResult(JsonNode post) {
        if (post == null || post.isMissingNode() || post.isNull()) {
            return null;
        }

        String title = normalize(post.path("RecruitPostName").asText());
        String location = normalize(post.path("LocationName").asText());
        if (StringUtils.hasText(location)) {
            title = title + "\uff5c" + location;
        }

        List<String> snippetParts = new ArrayList<>();
        String responsibility = normalize(post.path("Responsibility").asText().replace("\r", "\n"));
        String requirement = normalize(post.path("Requirement").asText().replace("\r", "\n"));
        String company = normalize(post.path("ComName").asText());
        String category = normalize(post.path("CategoryName").asText());
        String workYears = normalize(post.path("RequireWorkYearsName").asText());

        if (StringUtils.hasText(company)) {
            snippetParts.add("\u516c\u53f8\uff1a" + company);
        }
        if (StringUtils.hasText(responsibility)) {
            snippetParts.add("\u804c\u8d23\uff1a" + responsibility);
        }
        if (StringUtils.hasText(requirement)) {
            snippetParts.add("\u8981\u6c42\uff1a" + requirement);
        }
        if (StringUtils.hasText(category) || StringUtils.hasText(workYears)) {
            List<String> metaParts = new ArrayList<>();
            if (StringUtils.hasText(category)) {
                metaParts.add("\u7c7b\u522b\uff1a" + category);
            }
            if (StringUtils.hasText(workYears)) {
                metaParts.add("\u7ecf\u9a8c\uff1a" + workYears);
            }
            snippetParts.add(String.join("\uff1b", metaParts));
        }

        String link = normalize(post.path("PostURL").asText());
        if (link.startsWith("http://")) {
            link = "https://" + link.substring("http://".length());
        }
        if (!StringUtils.hasText(link)) {
            return null;
        }
        return new WebSearchResult(title, link, limit(String.join("\n", snippetParts), 1600));
    }

    private List<WebSearchResult> searchByteDanceJobs(String roleTitle) {
        Path frontendDir = resolveFrontendDir();
        if (frontendDir == null) {
            return List.of();
        }

        Path scriptPath = frontendDir.resolve("scripts").resolve("bytedance-job-source.mjs");
        if (!Files.exists(scriptPath)) {
            return List.of();
        }

        Process process = null;
        try {
            ProcessBuilder processBuilder = new ProcessBuilder(
                    "node",
                    scriptPath.toString(),
                    roleTitle,
                    "5");
            processBuilder.directory(frontendDir.toFile());
            processBuilder.redirectErrorStream(true);
            process = processBuilder.start();

            Process runningProcess = process;
            CompletableFuture<String> outputFuture =
                    CompletableFuture.supplyAsync(() -> readProcessOutput(runningProcess));

            boolean finished = process.waitFor(NODE_TIMEOUT.toSeconds(), TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                outputFuture.cancel(true);
                log.warn("ByteDance official search timed out for role: {}", roleTitle);
                return List.of();
            }

            String output = outputFuture.get(5, TimeUnit.SECONDS);
            if (process.exitValue() != 0) {
                log.warn("ByteDance official search exited with code {}: {}", process.exitValue(), output);
                return List.of();
            }

            List<ByteDanceJobPosting> postings = objectMapper.readValue(
                    extractJsonArray(output),
                    new TypeReference<List<ByteDanceJobPosting>>() {
                    });

            List<WebSearchResult> results = new ArrayList<>();
            for (ByteDanceJobPosting posting : postings) {
                WebSearchResult result = toWebSearchResult(posting);
                if (StringUtils.hasText(result.getTitle()) && StringUtils.hasText(result.getLink())) {
                    results.add(result);
                }
            }
            return results;
        } catch (Exception exception) {
            log.warn("ByteDance official job search failed: {}", exception.getMessage());
            return List.of();
        } finally {
            if (process != null && process.isAlive()) {
                process.destroyForcibly();
            }
        }
    }

    private List<WebSearchResult> searchShixiseng(String companyName, String roleTitle) {
        Map<String, WebSearchResult> merged = new LinkedHashMap<>();
        mergeResults(merged, searchShixisengByQuery(companyName + " " + roleTitle), 8);
        if (merged.isEmpty()) {
            mergeResults(merged, searchShixisengByQuery(companyName), 10);
        }
        return new ArrayList<>(merged.values());
    }

    private List<WebSearchResult> searchShixisengByQuery(String query) {
        if (!StringUtils.hasText(query)) {
            return List.of();
        }

        try {
            HttpResponse<byte[]> response = sendRequest("https://www.shixiseng.com/interns?keyword=" + encode(query));
            return parseShixisengSearchResults(decodeBody(response));
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private List<WebSearchResult> parseShixisengSearchResults(String html) {
        if (!StringUtils.hasText(html)) {
            return List.of();
        }

        Matcher matcher = SHIXISENG_SEARCH_CARD_PATTERN.matcher(html);
        List<WebSearchResult> results = new ArrayList<>();
        while (matcher.find()) {
            String link = normalize(decodeXml(matcher.group(2)));
            String title = sanitizeVisibleText(matcher.group(3));
            String company = sanitizeVisibleText(matcher.group(4));
            if (!StringUtils.hasText(title) && StringUtils.hasText(company)) {
                title = company + " 招聘";
            }
            if (!StringUtils.hasText(link) || !StringUtils.hasText(title)) {
                continue;
            }

            String snippet = "来源站点：实习僧";
            if (StringUtils.hasText(company)) {
                snippet = snippet + "\n公司：" + company;
            }
            results.add(new WebSearchResult(title, link, snippet));
            if (results.size() >= 8) {
                break;
            }
        }
        return results;
    }

    private List<WebSearchResult> enrichLandingPages(List<WebSearchResult> results) {
        if (results == null || results.isEmpty()) {
            return List.of();
        }

        List<WebSearchResult> enriched = new ArrayList<>();
        int limit = Math.min(results.size(), 8);
        for (int index = 0; index < results.size(); index++) {
            WebSearchResult original = results.get(index);
            if (index < limit) {
                enriched.add(enrichLandingPage(original));
            } else {
                enriched.add(original);
            }
        }
        return enriched;
    }

    private WebSearchResult enrichLandingPage(WebSearchResult result) {
        if (result == null || !StringUtils.hasText(result.getLink())) {
            return result;
        }

        try {
            LandingPage landingPage = fetchLandingPage(result.getLink());
            if (landingPage == null) {
                return result;
            }

            WebSearchResult enriched = new WebSearchResult(
                    chooseBetterTitle(result.getTitle(), landingPage.title()),
                    StringUtils.hasText(landingPage.url()) ? landingPage.url() : result.getLink(),
                    chooseBetterSnippet(result.getSnippet(), landingPage.snippet()));
            return enriched;
        } catch (Exception exception) {
            return result;
        }
    }

    private WebSearchResult toWebSearchResult(ByteDanceJobPosting posting) {
        String title = normalize(posting.getTitle());
        if (StringUtils.hasText(posting.getCity())) {
            title = title + "\uff5c" + posting.getCity();
        }

        List<String> snippetParts = new ArrayList<>();
        if (StringUtils.hasText(posting.getDescription())) {
            snippetParts.add("\u804c\u8d23\uff1a" + posting.getDescription());
        }
        if (StringUtils.hasText(posting.getRequirement())) {
            snippetParts.add("\u8981\u6c42\uff1a" + posting.getRequirement());
        }
        if (StringUtils.hasText(posting.getRecruitType())
                || StringUtils.hasText(posting.getCategory())
                || StringUtils.hasText(posting.getCode())) {
            List<String> metaParts = new ArrayList<>();
            if (StringUtils.hasText(posting.getRecruitType())) {
                metaParts.add("\u7c7b\u578b\uff1a" + posting.getRecruitType());
            }
            if (StringUtils.hasText(posting.getCategory())) {
                metaParts.add("\u7c7b\u522b\uff1a" + posting.getCategory());
            }
            if (StringUtils.hasText(posting.getCode())) {
                metaParts.add("\u804c\u4f4dID\uff1a" + posting.getCode());
            }
            snippetParts.add(String.join("\uff1b", metaParts));
        }

        String link = normalize(posting.getLink());
        if (!StringUtils.hasText(link)) {
            link = BYTE_DANCE_POSITION_URL;
        }
        return new WebSearchResult(title, link, limit(String.join("\n", snippetParts), 1200));
    }

    private LandingPage fetchLandingPage(String url) {
        try {
            HttpResponse<byte[]> response = sendRequest(url, LANDING_PAGE_TIMEOUT);
            String html = decodeBody(response);
            String finalUrl = normalize(response.uri().toString());

            String redirectUrl = extractRedirectUrl(html);
            if (StringUtils.hasText(redirectUrl)) {
                HttpResponse<byte[]> redirected = sendRequest(redirectUrl, LANDING_PAGE_TIMEOUT);
                html = decodeBody(redirected);
                finalUrl = normalize(redirected.uri().toString());
            }

            String title = extractPageTitle(html);
            String snippet = extractLandingSnippet(html);
            if (!StringUtils.hasText(title) && !StringUtils.hasText(snippet)) {
                return null;
            }
            return new LandingPage(finalUrl, title, snippet);
        } catch (Exception exception) {
            return null;
        }
    }

    private String extractRedirectUrl(String html) {
        if (!StringUtils.hasText(html)) {
            return "";
        }

        Matcher scriptMatcher = SCRIPT_REDIRECT_PATTERN.matcher(html);
        if (scriptMatcher.find()) {
            return normalize(decodeXml(scriptMatcher.group(1)));
        }

        Matcher refreshMatcher = META_REFRESH_PATTERN.matcher(html);
        if (refreshMatcher.find()) {
            return normalize(decodeXml(refreshMatcher.group(1)));
        }

        return "";
    }

    private String extractPageTitle(String html) {
        String ogTitle = extractFirstMatch(html, OG_TITLE_PATTERN);
        if (StringUtils.hasText(ogTitle)) {
            return sanitizeVisibleText(stripTags(decodeXml(ogTitle)));
        }
        String title = extractFirstMatch(html, TITLE_PATTERN);
        return sanitizeVisibleText(stripTags(decodeXml(title)));
    }

    private String extractLandingSnippet(String html) {
        List<String> parts = new ArrayList<>();

        String structured = extractStructuredJobText(html);
        if (StringUtils.hasText(structured)) {
            parts.add(structured);
        }

        String metaDescription = normalize(decodeXml(extractFirstMatch(html, META_DESCRIPTION_PATTERN)));
        if (StringUtils.hasText(metaDescription) && !containsSimilar(parts, metaDescription)) {
            parts.add(metaDescription);
        }

        String bodyExcerpt = extractRelevantBodyExcerpt(html);
        if (StringUtils.hasText(bodyExcerpt) && !containsSimilar(parts, bodyExcerpt)) {
            parts.add(bodyExcerpt);
        }

        return limit(String.join("\n", parts), 1800);
    }

    private String extractStructuredJobText(String html) {
        if (!StringUtils.hasText(html)) {
            return "";
        }

        Matcher nowcoderMatcher = NOWCODER_REQUIREMENTS_PATTERN.matcher(html);
        if (nowcoderMatcher.find()) {
            String requirements = normalize(decodeEscapedText(nowcoderMatcher.group(1)));
            String infos = normalize(decodeEscapedText(nowcoderMatcher.group(2)));
            List<String> parts = new ArrayList<>();
            if (StringUtils.hasText(infos)) {
                parts.add(infos);
            }
            if (StringUtils.hasText(requirements)) {
                parts.add(requirements);
            }
            return String.join("\n", parts);
        }

        Matcher shixisengMatcher = SHIXISENG_INFO_PATTERN.matcher(html);
        if (shixisengMatcher.find()) {
            return normalize(decodeEscapedText(shixisengMatcher.group(1)));
        }

        return "";
    }

    private String extractRelevantBodyExcerpt(String html) {
        if (!StringUtils.hasText(html)) {
            return "";
        }

        String body = normalizeBodyText(html);
        if (!StringUtils.hasText(body)) {
            return "";
        }

        for (String label : List.of(
                "岗位职责", "职位描述", "工作内容", "职责描述",
                "岗位要求", "任职要求", "任职资格", "职位要求")) {
            int index = body.indexOf(label);
            if (index >= 0) {
                return limit(body.substring(index), 900);
            }
        }

        return limit(body, 500);
    }

    private String normalizeBodyText(String html) {
        String cleaned = SCRIPT_STYLE_PATTERN.matcher(defaultValue(html)).replaceAll(" ");
        cleaned = BLOCK_TAG_PATTERN.matcher(cleaned).replaceAll("\n");
        cleaned = stripTags(decodeXml(cleaned));
        return cleaned.replaceAll("[ \\t\\x0B\\f\\r]+", " ")
                .replaceAll("\\n+", "\n")
                .trim();
    }

    private boolean containsSimilar(List<String> parts, String candidate) {
        String normalizedCandidate = lower(candidate);
        for (String part : parts) {
            String normalizedPart = lower(part);
            if (normalizedPart.contains(normalizedCandidate) || normalizedCandidate.contains(normalizedPart)) {
                return true;
            }
        }
        return false;
    }

    private String chooseBetterTitle(String originalTitle, String extractedTitle) {
        String cleanedOriginal = sanitizeVisibleText(originalTitle);
        String cleanedExtracted = sanitizeVisibleText(extractedTitle);
        if (!StringUtils.hasText(cleanedExtracted)) {
            return cleanedOriginal;
        }
        if (!StringUtils.hasText(cleanedOriginal)) {
            return cleanedExtracted;
        }
        if (containsPrivateUseCharacter(originalTitle) && !containsPrivateUseCharacter(extractedTitle)) {
            return cleanedExtracted;
        }
        if (isGenericCareerTitle(cleanedExtracted) && !isGenericCareerTitle(cleanedOriginal)) {
            return cleanedOriginal;
        }
        return cleanedExtracted.length() >= cleanedOriginal.length() - 8 ? cleanedExtracted : cleanedOriginal;
    }

    private String chooseBetterSnippet(String originalSnippet, String extractedSnippet) {
        String cleanedOriginal = sanitizeVisibleText(originalSnippet);
        String cleanedExtracted = sanitizeVisibleText(extractedSnippet);
        if (!StringUtils.hasText(cleanedExtracted)) {
            return cleanedOriginal;
        }
        if (!StringUtils.hasText(cleanedOriginal)) {
            return cleanedExtracted;
        }
        if (containsJobRequirementSignal(cleanedExtracted) && !containsJobRequirementSignal(cleanedOriginal)) {
            return cleanedExtracted;
        }
        return cleanedExtracted.length() > cleanedOriginal.length() ? cleanedExtracted : cleanedOriginal;
    }

    private boolean containsJobRequirementSignal(String text) {
        String normalized = lower(text);
        for (String signal : List.of(
                "岗位职责", "职位描述", "工作内容", "岗位要求",
                "任职要求", "任职资格", "职位要求", "职责", "要求")) {
            if (normalized.contains(signal.toLowerCase(Locale.ROOT))) {
                return true;
            }
        }
        return false;
    }

    private Path resolveFrontendDir() {
        Path cwd = Path.of("").toAbsolutePath().normalize();
        List<Path> candidates = List.of(
                cwd.resolve("frontend"),
                cwd.resolve("..").resolve("frontend"));
        for (Path candidate : candidates) {
            if (Files.exists(candidate.resolve("package.json"))) {
                return candidate.normalize();
            }
        }
        return null;
    }

    private String extractJsonArray(String output) {
        if (!StringUtils.hasText(output)) {
            return "[]";
        }
        int start = output.indexOf('[');
        int end = output.lastIndexOf(']');
        if (start >= 0 && end >= start) {
            return output.substring(start, end + 1);
        }
        return "[]";
    }

    private String readProcessOutput(Process process) {
        try {
            return new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException exception) {
            return "";
        }
    }

    private List<WebSearchResult> fetchRssResults(String query) {
        try {
            HttpResponse<byte[]> response = sendRequest("https://www.bing.com/search?format=rss&q=" + encode(query));
            return parseRss(new String(response.body(), StandardCharsets.UTF_8));
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private List<WebSearchResult> fetchHtmlResults(String query) {
        try {
            HttpResponse<byte[]> response = sendRequest("https://cn.bing.com/search?q=" + encode(query));
            return parseHtml(new String(response.body(), StandardCharsets.UTF_8));
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private List<WebSearchResult> fetchSogouResults(String query) {
        try {
            HttpResponse<byte[]> response = sendRequest("https://www.sogou.com/web?query=" + encode(query));
            return parseSogouHtml(new String(response.body(), StandardCharsets.UTF_8));
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private HttpResponse<byte[]> sendRequest(String url) throws IOException, InterruptedException {
        return sendRequest(url, Duration.ofSeconds(12));
    }

    private HttpResponse<byte[]> sendRequest(String url, Duration timeout) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(timeout)
                .header("User-Agent", "Mozilla/5.0 CareerPilot/1.0")
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }

    private String decodeBody(HttpResponse<byte[]> response) {
        if (response == null || response.body() == null) {
            return "";
        }

        String contentType = response.headers().firstValue("content-type").orElse("");
        String lowerContentType = contentType.toLowerCase(Locale.ROOT);
        if (lowerContentType.contains("charset=gbk") || lowerContentType.contains("charset=gb2312")) {
            return new String(response.body(), java.nio.charset.Charset.forName("GB18030"));
        }
        return new String(response.body(), StandardCharsets.UTF_8);
    }

    private List<WebSearchResult> parseRss(String xml) {
        if (!StringUtils.hasText(xml)) {
            return List.of();
        }

        Matcher matcher = RSS_ITEM_PATTERN.matcher(xml);
        List<WebSearchResult> results = new ArrayList<>();
        while (matcher.find()) {
            String title = normalize(decodeXml(matcher.group(1)));
            String link = normalize(decodeXml(matcher.group(2)));
            String description = normalize(decodeXml(matcher.group(3)));
            if (StringUtils.hasText(title) && StringUtils.hasText(link)) {
                results.add(new WebSearchResult(title, link, description));
            }
        }
        return results;
    }

    private List<WebSearchResult> parseHtml(String html) {
        if (!StringUtils.hasText(html)) {
            return List.of();
        }

        Matcher matcher = HTML_RESULT_PATTERN.matcher(html);
        List<WebSearchResult> results = new ArrayList<>();
        while (matcher.find()) {
            String link = normalize(decodeXml(matcher.group(1)));
            String title = normalize(stripTags(decodeXml(matcher.group(2))));
            String description = normalize(stripTags(decodeXml(matcher.group(3))));
            if (StringUtils.hasText(title) && StringUtils.hasText(link)) {
                results.add(new WebSearchResult(title, link, description));
            }
        }
        return results;
    }

    private List<WebSearchResult> parseSogouHtml(String html) {
        if (!StringUtils.hasText(html)) {
            return List.of();
        }
        if (html.contains("\u9a8c\u8bc1\u7801") && html.contains("\u786e\u8ba4\u8fd9\u4e9b\u8bf7\u6c42")) {
            return List.of();
        }

        Matcher matcher = SOGOU_RESULT_PATTERN.matcher(html);
        List<WebSearchResult> results = new ArrayList<>();
        while (matcher.find()) {
            String rawLink = normalize(decodeXml(matcher.group(1)));
            String link = rawLink.startsWith("/link?")
                    ? "https://www.sogou.com" + rawLink
                    : rawLink;
            String title = normalize(stripTags(decodeXml(matcher.group(2))));
            String description = normalize(stripTags(decodeXml(matcher.group(3))));
            String source = normalize(stripTags(decodeXml(matcher.group(4))));
            if (StringUtils.hasText(source)) {
                description = normalize(description + " 来源站点：" + source);
            }
            if (StringUtils.hasText(title) && StringUtils.hasText(link)) {
                results.add(new WebSearchResult(title, link, description));
            }
        }
        return results;
    }

    private String encode(String value) {
        return java.net.URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private String stripTags(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return TAG_PATTERN.matcher(value).replaceAll(" ");
    }

    private String sanitizeVisibleText(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        String decoded = decodeXml(value);
        decoded = PRIVATE_USE_CHAR_PATTERN.matcher(decoded).replaceAll("");
        return normalize(stripTags(decoded));
    }

    private String normalize(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }
        return value.replaceAll("\\s+", " ").trim();
    }

    private String lower(String value) {
        return normalize(value).toLowerCase(Locale.ROOT);
    }

    private String defaultValue(String value) {
        return value == null ? "" : value;
    }

    private String limit(String value, int maxLength) {
        if (!StringUtils.hasText(value) || value.length() <= maxLength) {
            return value == null ? "" : value;
        }
        return value.substring(0, maxLength);
    }

    private String extractFirstMatch(String html, Pattern pattern) {
        if (!StringUtils.hasText(html)) {
            return "";
        }
        Matcher matcher = pattern.matcher(html);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return "";
    }

    private String decodeEscapedText(String value) {
        if (!StringUtils.hasText(value)) {
            return "";
        }

        String decoded = value
                .replace("\\/", "/")
                .replace("\\n", "\n")
                .replace("\\r", "\n")
                .replace("\\t", " ")
                .replace("\\\"", "\"")
                .replace("\\'", "'");

        Matcher matcher = Pattern.compile("\\\\u([0-9a-fA-F]{4})").matcher(decoded);
        StringBuffer buffer = new StringBuffer();
        while (matcher.find()) {
            char character = (char) Integer.parseInt(matcher.group(1), 16);
            matcher.appendReplacement(buffer, Matcher.quoteReplacement(String.valueOf(character)));
        }
        matcher.appendTail(buffer);
        return buffer.toString().replace("\\\\", "\\");
    }

    private String decodeXml(String value) {
        if (value == null) {
            return "";
        }
        return HtmlUtils.htmlUnescape(value);
    }

    private boolean containsPrivateUseCharacter(String value) {
        return StringUtils.hasText(value) && PRIVATE_USE_CHAR_PATTERN.matcher(value).find();
    }

    private boolean isGenericCareerTitle(String value) {
        String normalized = lower(value);
        return normalized.contains("\u5c97\u4f4d\u8be6\u60c5")
                || normalized.equals("\u817e\u8baf\u62db\u8058")
                || normalized.equals("\u5b9e\u4e60\u50e7")
                || normalized.equals("\u725b\u5ba2\u7f51");
    }

    private record LandingPage(String url, String title, String snippet) {
    }

    private static class ByteDanceJobPosting {
        private String id;
        private String title;
        private String link;
        private String city;
        private String recruitType;
        private String category;
        private String code;
        private String description;
        private String requirement;

        public String getId() {
            return id;
        }

        public void setId(String id) {
            this.id = id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getLink() {
            return link;
        }

        public void setLink(String link) {
            this.link = link;
        }

        public String getCity() {
            return city;
        }

        public void setCity(String city) {
            this.city = city;
        }

        public String getRecruitType() {
            return recruitType;
        }

        public void setRecruitType(String recruitType) {
            this.recruitType = recruitType;
        }

        public String getCategory() {
            return category;
        }

        public void setCategory(String category) {
            this.category = category;
        }

        public String getCode() {
            return code;
        }

        public void setCode(String code) {
            this.code = code;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }

        public String getRequirement() {
            return requirement;
        }

        public void setRequirement(String requirement) {
            this.requirement = requirement;
        }
    }
}
