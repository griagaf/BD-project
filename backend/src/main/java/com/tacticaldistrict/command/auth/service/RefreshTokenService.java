package com.tacticaldistrict.command.auth.service;

import com.tacticaldistrict.command.auth.entity.RefreshTokenEntity;
import com.tacticaldistrict.command.auth.repository.RefreshTokenRepository;
import com.tacticaldistrict.command.security.config.JwtProperties;
import com.tacticaldistrict.command.security.model.UserPrincipal;
import com.tacticaldistrict.command.user.service.TacticalUserDetailsService;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private static final int TOKEN_BYTES = 48;

    private final SecureRandom secureRandom = new SecureRandom();
    private final RefreshTokenRepository refreshTokenRepository;
    private final TacticalUserDetailsService userDetailsService;
    private final JwtProperties jwtProperties;

    @Transactional
    public String create(Long userId, String userAgent, String ipAddress) {
        String rawToken = generateRawToken();
        RefreshTokenEntity entity = new RefreshTokenEntity();
        entity.setUserId(userId);
        entity.setTokenHash(hash(rawToken));
        entity.setIssuedAt(Instant.now());
        entity.setExpiresAt(Instant.now().plusSeconds(jwtProperties.refreshTtlDays() * 24 * 60 * 60));
        entity.setUserAgent(userAgent);
        entity.setIpAddress(ipAddress);
        refreshTokenRepository.save(entity);
        return rawToken;
    }

    @Transactional
    public RotationResult rotate(String rawToken, String userAgent, String ipAddress) {
        RefreshTokenEntity current = refreshTokenRepository.findByTokenHash(hash(rawToken))
                .orElseThrow(() -> new BadCredentialsException("Invalid refresh token"));

        if (current.getRevokedAt() != null || current.getExpiresAt().isBefore(Instant.now())) {
            throw new BadCredentialsException("Invalid refresh token");
        }

        UserPrincipal principal = userDetailsService.loadPrincipalByUserId(current.getUserId());
        String replacement = create(principal.userId(), userAgent, ipAddress);

        current.setRevokedAt(Instant.now());
        current.setReplacedByHash(hash(replacement));

        return new RotationResult(principal, replacement);
    }

    @Transactional
    public void revoke(String rawToken) {
        refreshTokenRepository.findByTokenHash(hash(rawToken)).ifPresent(token -> {
            if (token.getRevokedAt() == null) {
                token.setRevokedAt(Instant.now());
            }
        });
    }

    public String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private String generateRawToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    public record RotationResult(
            UserPrincipal principal,
            String refreshToken
    ) {
    }
}
