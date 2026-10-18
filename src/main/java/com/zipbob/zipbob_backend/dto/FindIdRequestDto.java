package com.zipbob.zipbob_backend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
/*
아이디 찾기 요청
{
  "email": "zipbob",
  "emailAdr": "gmail.com",
  "verificationToken": "인증번호 확인 후 발급받은 토큰"
}
*/
public class FindIdRequestDto {

    // 이메일 @ 앞부분
    private String email;
    // 이메일 도메인
    private String emailAdr;
    // 인증번호 확인 성공 후 발급받은 인증 증명
    private String verificationToken;
}