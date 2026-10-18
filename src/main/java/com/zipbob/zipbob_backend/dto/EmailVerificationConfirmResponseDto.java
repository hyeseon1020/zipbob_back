package com.zipbob.zipbob_backend.dto;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
// 인증번호 인증 성공 응답
public class EmailVerificationConfirmResponseDto {

    // 회원가입 등 실제 작업에서 제출할 인증 증명
    private final String verificationToken;

    private final LocalDateTime tokenExpiresDt;
    private final long expiresInSeconds;
    private final String message;

    public EmailVerificationConfirmResponseDto(
            String verificationToken,
            LocalDateTime tokenExpiresDt,
            long expiresInSeconds
    ) {
        this.verificationToken = verificationToken;
        this.tokenExpiresDt = tokenExpiresDt;
        this.expiresInSeconds = expiresInSeconds;
        this.message = "이메일 인증이 완료되었습니다.";
    }
}