package com.careerpilot.dto;

public class AuthResponse {

    private final String token;
    private final Long userId;
    private final String username;
    private final String phone;

    public AuthResponse(String token, Long userId, String username, String phone) {
        this.token = token;
        this.userId = userId;
        this.username = username;
        this.phone = phone;
    }

    public String getToken() {
        return token;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getPhone() {
        return phone;
    }
}
