package com.tacticaldistrict.command.security.service;

import com.tacticaldistrict.command.security.config.JwtProperties;
import com.tacticaldistrict.command.security.model.RoleCode;
import com.tacticaldistrict.command.security.model.UserPrincipal;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private static final String ISSUER = "tactical-district-command";

    private final SecretKey secretKey;
    private final JwtProperties jwtProperties;

    public JwtService(JwtProperties jwtProperties) {
        this.jwtProperties = jwtProperties;
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.secret().getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(UserPrincipal principal) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plusSeconds(jwtProperties.accessTtlMinutes() * 60);

        JwtBuilder builder = Jwts.builder()
                .issuer(ISSUER)
                .subject(principal.username())
                .id(UUID.randomUUID().toString())
                .issuedAt(Date.from(issuedAt))
                .expiration(Date.from(expiresAt))
                .claim("typ", "access")
                .claim("uid", principal.userId())
                .claim("roles", principal.roles().stream()
                        .map(RoleCode::name)
                        .sorted()
                        .collect(Collectors.joining(",")))
                .signWith(secretKey);

        if (principal.soldierId() != null) {
            builder.claim("sid", principal.soldierId());
        }

        return builder.compact();
    }

    public Optional<String> parseUsername(String token) {
        try {
            Claims claims = Jwts.parser()
                    .requireIssuer(ISSUER)
                    .verifyWith(secretKey)
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();

            if (!"access".equals(claims.get("typ", String.class))) {
                return Optional.empty();
            }

            return Optional.of(claims.getSubject());
        } catch (RuntimeException exception) {
            return Optional.empty();
        }
    }

    public Set<RoleCode> parseRolesUnsafe(String token) {
        Claims claims = Jwts.parser()
                .requireIssuer(ISSUER)
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        String rolesClaim = claims.get("roles", String.class);
        return rolesClaim == null || rolesClaim.isBlank()
                ? Set.of()
                : Arrays.stream(rolesClaim.split(","))
                        .map(RoleCode::valueOf)
                        .collect(Collectors.toUnmodifiableSet());
    }
}
