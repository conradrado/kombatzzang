package com.sprint.mission.matzzang.auth.jwt;

import com.sprint.mission.matzzang.user.constants.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtBuilder;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class JwtTokenProvider {

    private static final String CLAIM_ROLE = "role";

    private final SecretKey key;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration-ms}") long accessTokenExpirationMs,
            @Value("${jwt.refresh-token-expiration-ms}") long refreshTokenExpirationMs
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMs = accessTokenExpirationMs;
        this.refreshTokenExpirationMs = refreshTokenExpirationMs;
    }

    public long getAccessTokenExpirationMs() {
        return accessTokenExpirationMs;
    }

    public long getRefreshTokenExpirationMs() {
        return refreshTokenExpirationMs;
    }

    public String generateAccessToken(Long userId, UserRole role) {
        return generateToken(userId, role, accessTokenExpirationMs);
    }

    public String generateRefreshToken(Long userId) {
        return generateToken(userId, null, refreshTokenExpirationMs);
    }

    public JwtPrincipal parse(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        Long userId = Long.valueOf(claims.getSubject());
        String roleValue = claims.get(CLAIM_ROLE, String.class);
        UserRole role = roleValue != null ? UserRole.valueOf(roleValue) : null;
        return new JwtPrincipal(userId, role);
    }

    private String generateToken(Long userId, UserRole role, long expirationMs) {
        Instant now = Instant.now();
        JwtBuilder builder = Jwts.builder()
                .subject(String.valueOf(userId))
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusMillis(expirationMs)));

        if (role != null) {
            builder.claim(CLAIM_ROLE, role.name());
        }

        return builder.signWith(key).compact();
    }
}
