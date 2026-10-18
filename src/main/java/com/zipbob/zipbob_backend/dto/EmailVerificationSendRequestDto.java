package com.zipbob.zipbob_backend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
/*
인증번호 발송 요청 (이메일·인증 목적)
{
  "email": "test",
  "emailAdr": "gmail.com",
  "purpose": "SIGNUP"
}
*/
public class EmailVerificationSendRequestDto {

    // 이메일 @ 앞부분
    private String email;

    // 이메일 도메인
    private String emailAdr;

    // SIGNUP / FIND_ID / RESET_PASSWORD
    private String purpose;
}