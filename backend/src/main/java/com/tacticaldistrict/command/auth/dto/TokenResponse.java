package com.tacticaldistrict.command.auth.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        long expiresIn,
        CurrentUserResponse user
) {
}

