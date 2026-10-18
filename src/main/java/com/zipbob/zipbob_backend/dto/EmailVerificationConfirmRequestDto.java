package com.zipbob.zipbob_backend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
/*
인증번호 확인 요청 (인증 요청 ID·인증번호)
{
  "emailVerificationId": 1,
  "verificationCode": "012345"
}
*/
public class EmailVerificationConfirmRequestDto {

    // 발송 응답에서 받은 인증 요청 ID
    private Long emailVerificationId;

    // 이메일로 받은 인증번호
    private String verificationCode;
}