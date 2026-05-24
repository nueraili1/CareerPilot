package com.careerpilot.service;

import com.careerpilot.dto.SendCodeResponse;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class VerificationCodeService {

    private static final Logger log = LoggerFactory.getLogger(VerificationCodeService.class);
    private static final String CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final int CODE_LENGTH = 4;
    private static final int RESEND_SECONDS = 10;
    private static final int EXPIRES_SECONDS = 300;

    private final SecureRandom secureRandom = new SecureRandom();
    private final StringRedisTemplate redisTemplate;
    private final Map<String, CachedCode> localCache = new ConcurrentHashMap<>();

    public VerificationCodeService(ObjectProvider<StringRedisTemplate> redisTemplateProvider) {
        this.redisTemplate = redisTemplateProvider.getIfAvailable();
    }

    public SendCodeResponse send(String phone, String purpose) {
        String normalizedPurpose = normalizePurpose(purpose);
        String code = generateCode();
        cacheCode(key(phone, normalizedPurpose), code);
        log.info("Verification code generated: phone={}, purpose={}, code={}", maskPhone(phone), normalizedPurpose, code);
        return new SendCodeResponse(phone, code, RESEND_SECONDS, EXPIRES_SECONDS);
    }

    public void verify(String phone, String purpose, String code) {
        String normalizedPurpose = normalizePurpose(purpose);
        String cacheKey = key(phone, normalizedPurpose);
        String cachedCode = getCode(cacheKey);
        if (!StringUtils.hasText(cachedCode)) {
            throw new IllegalArgumentException("验证码已过期，请重新发送");
        }
        if (!cachedCode.equalsIgnoreCase(code == null ? "" : code.trim())) {
            throw new IllegalArgumentException("验证码不正确");
        }
        deleteCode(cacheKey);
    }

    private void cacheCode(String key, String code) {
        try {
            if (redisTemplate != null) {
                redisTemplate.opsForValue().set(key, code, Duration.ofSeconds(EXPIRES_SECONDS));
                return;
            }
        } catch (Exception exception) {
            log.warn("Redis unavailable, fallback to local verification cache: {}", exception.getMessage());
        }
        localCache.put(key, new CachedCode(code, Instant.now().plusSeconds(EXPIRES_SECONDS)));
    }

    private String getCode(String key) {
        try {
            if (redisTemplate != null) {
                return redisTemplate.opsForValue().get(key);
            }
        } catch (Exception exception) {
            log.warn("Redis read failed, fallback to local verification cache: {}", exception.getMessage());
        }

        CachedCode cachedCode = localCache.get(key);
        if (cachedCode == null) {
            return null;
        }
        if (cachedCode.expiresAt().isBefore(Instant.now())) {
            localCache.remove(key);
            return null;
        }
        return cachedCode.code();
    }

    private void deleteCode(String key) {
        try {
            if (redisTemplate != null) {
                redisTemplate.delete(key);
            }
        } catch (Exception exception) {
            log.warn("Redis delete failed: {}", exception.getMessage());
        }
        localCache.remove(key);
    }

    private String generateCode() {
        StringBuilder code = new StringBuilder(CODE_LENGTH);
        for (int index = 0; index < CODE_LENGTH; index++) {
            code.append(CHARS.charAt(secureRandom.nextInt(CHARS.length())));
        }
        return code.toString();
    }

    private String key(String phone, String purpose) {
        return "careerpilot:verify:" + purpose + ":" + phone;
    }

    private String normalizePurpose(String purpose) {
        String normalized = purpose == null ? "" : purpose.trim().toUpperCase();
        if (!"REGISTER".equals(normalized) && !"RESET_PASSWORD".equals(normalized)) {
            throw new IllegalArgumentException("验证码场景不正确");
        }
        return normalized;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 7) {
            return "unknown";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private record CachedCode(String code, Instant expiresAt) {
    }
}
