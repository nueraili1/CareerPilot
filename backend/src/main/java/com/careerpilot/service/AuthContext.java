package com.careerpilot.service;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Optional;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Component
public class AuthContext {

    private final TokenService tokenService;

    public AuthContext(TokenService tokenService) {
        this.tokenService = tokenService;
    }

    public Optional<TokenPayload> currentUser() {
        ServletRequestAttributes attributes = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return Optional.empty();
        }

        HttpServletRequest request = attributes.getRequest();
        String authorization = request.getHeader("Authorization");
        if (!StringUtils.hasText(authorization) || !authorization.startsWith("Bearer ")) {
            return Optional.empty();
        }

        try {
            return Optional.of(tokenService.parse(authorization.substring(7)));
        } catch (IllegalArgumentException exception) {
            return Optional.empty();
        }
    }

    public TokenPayload requireUser() {
        return currentUser().orElseThrow(() -> new IllegalArgumentException("请先登录"));
    }
}
