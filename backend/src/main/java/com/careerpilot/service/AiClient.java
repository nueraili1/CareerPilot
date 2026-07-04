package com.careerpilot.service;

import com.careerpilot.config.AiProperties;
import com.careerpilot.dto.AiRuntimeConfig;
import com.careerpilot.dto.ChatMessage;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cdimascio.dotenv.Dotenv;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

@Service
public class AiClient {

    private static final Logger log = LoggerFactory.getLogger(AiClient.class);

    private final AiProperties properties;
    private final ObjectMapper objectMapper;
    private final WebClient webClient;
    private final Dotenv dotenv;

    public AiClient(AiProperties properties, ObjectMapper objectMapper, WebClient.Builder webClientBuilder) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.webClient = webClientBuilder.build();
        this.dotenv = Dotenv.configure().ignoreIfMissing().load();
    }

    public boolean isConfigured() {
        return isConfigured(null);
    }

    public boolean isConfigured(AiRuntimeConfig runtimeConfig) {
        return StringUtils.hasText(getApiKey(runtimeConfig))
                && StringUtils.hasText(getBaseUrl(runtimeConfig))
                && StringUtils.hasText(getModel(runtimeConfig));
    }

    public boolean isRuntimeConfigured(AiRuntimeConfig runtimeConfig) {
        return runtimeConfig != null
                && StringUtils.hasText(runtimeConfig.getApiKey())
                && StringUtils.hasText(runtimeConfig.getBaseUrl())
                && StringUtils.hasText(runtimeConfig.getModel());
    }

    public String getBaseUrl() {
        return getBaseUrl(null);
    }

    public String getBaseUrl(AiRuntimeConfig runtimeConfig) {
        if (runtimeConfig != null && StringUtils.hasText(runtimeConfig.getBaseUrl())) {
            return runtimeConfig.getBaseUrl();
        }
        return resolve("AI_BASE_URL", properties.getBaseUrl());
    }

    public String getModel() {
        return getModel(null);
    }

    public String getModel(AiRuntimeConfig runtimeConfig) {
        if (runtimeConfig != null && StringUtils.hasText(runtimeConfig.getModel())) {
            return runtimeConfig.getModel();
        }
        return resolve("AI_MODEL", properties.getModel());
    }

    public String chat(String systemPrompt, String userPrompt) {
        return chat(systemPrompt, userPrompt, null);
    }

    public String chat(String systemPrompt, String userPrompt, AiRuntimeConfig runtimeConfig) {
        return chat(systemPrompt, userPrompt, List.of(), runtimeConfig);
    }

    public String chat(String systemPrompt, String contextPrompt, List<ChatMessage> messages, AiRuntimeConfig runtimeConfig) {
        List<Map<String, Object>> requestMessages = new ArrayList<>();
        requestMessages.add(Map.of("role", "system", "content", systemPrompt));
        if (StringUtils.hasText(contextPrompt)) {
            requestMessages.add(Map.of("role", "user", "content", contextPrompt));
        }
        for (ChatMessage message : messages) {
            requestMessages.add(Map.of(
                    "role", normalizeRole(message.getRole()),
                    "content", defaultValue(message.getContent())));
        }
        return sendMessages(requestMessages, runtimeConfig, contextPrompt == null ? 0 : contextPrompt.length());
    }

    public String chatWithUserContent(String systemPrompt, List<Map<String, Object>> userContent, AiRuntimeConfig runtimeConfig) {
        List<Map<String, Object>> requestMessages = new ArrayList<>();
        requestMessages.add(Map.of("role", "system", "content", systemPrompt));
        requestMessages.add(Map.of("role", "user", "content", userContent));
        return sendMessages(requestMessages, runtimeConfig, systemPrompt == null ? 0 : systemPrompt.length());
    }

    private String sendMessages(List<Map<String, Object>> requestMessages, AiRuntimeConfig runtimeConfig, int promptChars) {
        String apiKey = getApiKey(runtimeConfig);
        if (!StringUtils.hasText(apiKey)) {
            throw new IllegalStateException("AI_API_KEY is not configured");
        }

        String baseUrl = trimTrailingSlash(getBaseUrl(runtimeConfig));
        String model = getModel(runtimeConfig);
        String endpoint = baseUrl + "/chat/completions";
        long startedAt = System.currentTimeMillis();

        Map<String, Object> payload = Map.of(
                "model", model,
                "temperature", 0.2,
                "messages", requestMessages
        );

        String responseBody = webClient.post()
                .uri(endpoint)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(payload)
                .retrieve()
                .bodyToMono(String.class)
                .timeout(Duration.ofSeconds(properties.getTimeoutSeconds()))
                .block();

        String content = extractContent(responseBody);
        log.info("AI chat completed: model={}, promptChars={}, responseChars={}, costMs={}",
                model, promptChars, content.length(), System.currentTimeMillis() - startedAt);
        return content;
    }

    private String getApiKey(AiRuntimeConfig runtimeConfig) {
        if (runtimeConfig != null && StringUtils.hasText(runtimeConfig.getApiKey())) {
            return runtimeConfig.getApiKey();
        }
        return resolve("AI_API_KEY", properties.getApiKey());
    }

    private String normalizeRole(String role) {
        if ("assistant".equals(role)) {
            return "assistant";
        }
        return "user";
    }

    private String extractContent(String responseBody) {
        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode contentNode = root.path("choices").path(0).path("message").path("content");
            if (contentNode.isTextual()) {
                return contentNode.asText();
            }
            if (contentNode.isArray()) {
                StringBuilder builder = new StringBuilder();
                for (JsonNode item : contentNode) {
                    if (item.path("type").asText().equals("text")) {
                        builder.append(item.path("text").asText());
                    }
                }
                return builder.toString();
            }
            return contentNode.asText();
        } catch (Exception exception) {
            throw new IllegalStateException("AI response parse failed", exception);
        }
    }

    private String resolve(String envName, String configuredValue) {
        String systemValue = System.getenv(envName);
        if (StringUtils.hasText(systemValue)) {
            return systemValue;
        }
        String dotenvValue = dotenv.get(envName);
        if (StringUtils.hasText(dotenvValue)) {
            return dotenvValue;
        }
        return configuredValue;
    }

    private String trimTrailingSlash(String value) {
        if (value == null) {
            return "";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }

    private String defaultValue(String value) {
        return value == null ? "" : value;
    }
}
