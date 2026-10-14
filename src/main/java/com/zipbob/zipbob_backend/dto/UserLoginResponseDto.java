package com.zipbob.zipbob_backend.dto;

import com.zipbob.zipbob_backend.entity.User;
import lombok.Getter;

@Getter
// 회원 정보 + 토큰과 액세스 토큰 유효기간
public class UserLoginResponseDto {

    private final String message;
    private final String userId;
    private final String userName;

    private final String accessToken;
    private final String refreshToken;

    private final String tokenType;

    // 액세스 토큰 유효기간, 단위: 초
    private final long expiresIn;

    public UserLoginResponseDto(
            User user,
            String accessToken,
            String refreshToken,
            long expiresIn
    ) {
        this.message = "로그인되었습니다.";
        this.userId = user.getUserId();
        this.userName = user.getUserName();

        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.tokenType = "Bearer";
        this.expiresIn = expiresIn;
    }
}