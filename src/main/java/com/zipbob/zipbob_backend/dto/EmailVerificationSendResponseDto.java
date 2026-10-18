package com.zipbob.zipbob_backend.dto;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
// 인증번호 발송 성공 응답
public class EmailVerificationSendResponseDto {

    private final Long emailVerificationId;
    private final LocalDateTime expiresDt;
    private final long expiresInSeconds;
    private final String message;

    public EmailVerificationSendResponseDto(
            Long emailVerificationId,
            LocalDateTime expiresDt,
            long expiresInSeconds
    ) {
        this.emailVerificationId = emailVerificationId;
        this.expiresDt = expiresDt;
        this.expiresInSeconds = expiresInSeconds;
        this.message = "인증번호를 발송했습니다.";
    }
}