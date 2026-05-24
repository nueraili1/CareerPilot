package com.careerpilot.dto;

public class UserProfile {

    private final Long userId;
    private final String username;
    private final String phone;

    public UserProfile(Long userId, String username, String phone) {
        this.userId = userId;
        this.username = username;
        this.phone = phone;
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
