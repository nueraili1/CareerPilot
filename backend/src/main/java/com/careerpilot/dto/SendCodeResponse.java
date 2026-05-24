package com.careerpilot.dto;

public class SendCodeResponse {

    private final String phone;
    private final String code;
    private final int resendSeconds;
    private final int expiresSeconds;

    public SendCodeResponse(String phone, String code, int resendSeconds, int expiresSeconds) {
        this.phone = phone;
        this.code = code;
        this.resendSeconds = resendSeconds;
        this.expiresSeconds = expiresSeconds;
    }

    public String getPhone() {
        return phone;
    }

    public String getCode() {
        return code;
    }

    public int getResendSeconds() {
        return resendSeconds;
    }

    public int getExpiresSeconds() {
        return expiresSeconds;
    }
}
