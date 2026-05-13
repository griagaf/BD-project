package com.tacticaldistrict.command.security.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security.jwt")
public record JwtProperties(
        String secret,
        long accessTtlMinutes,
        long refreshTtlDays
) {
}

