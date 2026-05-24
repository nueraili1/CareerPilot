package com.careerpilot.service;

import com.careerpilot.config.AuthProperties;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class TokenService {

    private final AuthProperties properties;
    private final ObjectMapper objectMapper;

    public TokenService(AuthProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
    }

    public String createToken(Long userId, String username) {
        try {
            long expiresAt = Instant.now()
                    .plus(properties.getTokenExpireHours(), ChronoUnit.HOURS)
                    .getEpochSecond();
            String header = encodeJson(Map.of("alg", "HS256", "typ", "JWT"));
            String payload = encodeJson(Map.of("sub", userId, "username", username, "exp", expiresAt));
            String unsignedToken = header + "." + payload;
            return unsignedToken + "." + sign(unsignedToken);
        } catch (Exception exception) {
            throw new IllegalStateException("Token 生成失败", exception);
        }
    }

    public TokenPayload parse(String token) {
        if (!StringUtils.hasText(token)) {
            throw new IllegalArgumentException("Token 不能为空");
        }

        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3) {
                throw new IllegalArgumentException("Token 格式错误");
            }
            String unsignedToken = parts[0] + "." + parts[1];
            if (!sign(unsignedToken).equals(parts[2])) {
                throw new IllegalArgumentException("Token 签名无效");
            }

            JsonNode payload = objectMapper.readTree(Base64.getUrlDecoder().decode(parts[1]));
            long expiresAt = payload.path("exp").asLong();
            if (expiresAt < Instant.now().getEpochSecond()) {
                throw new IllegalArgumentException("登录已过期，请重新登录");
            }
            return new TokenPayload(payload.path("sub").asLong(), payload.path("username").asText(), expiresAt);
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("Token 解析失败");
        }
    }

    private String encodeJson(Map<String, Object> value) throws Exception {
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(objectMapper.writeValueAsBytes(value));
    }

    private String sign(String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(properties.getJwtSecret().getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    }
}
