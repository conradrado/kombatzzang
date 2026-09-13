package com.sprint.mission.matzzang.auth.dto;

public record TokenResponse(
        String accessToken,
        String refreshToken,
        String tokenType,
        long expiresIn
) {

    public static TokenResponse of(String accessToken, String refreshToken, long accessTokenExpirationMs) {
        return new TokenResponse(accessToken, refreshToken, "Bearer", accessTokenExpirationMs / 1000);
    }
}
