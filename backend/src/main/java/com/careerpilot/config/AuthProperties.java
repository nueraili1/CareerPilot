package com.careerpilot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "app.auth")
public class AuthProperties {

    private String jwtSecret = "careerpilot-dev-secret-change-me";
    private long tokenExpireHours = 72;

    public String getJwtSecret() {
        return jwtSecret;
    }

    public void setJwtSecret(String jwtSecret) {
        this.jwtSecret = jwtSecret;
    }

    public long getTokenExpireHours() {
        return tokenExpireHours;
    }

    public void setTokenExpireHours(long tokenExpireHours) {
        this.tokenExpireHours = tokenExpireHours;
    }
}
