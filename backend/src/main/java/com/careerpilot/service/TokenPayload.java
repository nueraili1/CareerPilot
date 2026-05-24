package com.careerpilot.service;

public record TokenPayload(Long userId, String username, long expiresAt) {
}
