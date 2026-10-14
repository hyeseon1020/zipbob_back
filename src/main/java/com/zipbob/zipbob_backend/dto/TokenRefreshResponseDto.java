package com.zipbob.zipbob_backend.dto;

import lombok.Getter;

@Getter
// 토큰 재발급 응답 (토큰정보)
public class TokenRefreshResponseDto {

    private final String accessToken;
    private final String refreshToken;
    private final String tokenType;
    private final long expiresIn;

    public TokenRefreshResponseDto(
            String accessToken,
            String refreshToken,
            long expiresIn
    ) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenType = "Bearer";
        this.expiresIn = expiresIn;
    }
}