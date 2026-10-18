package com.zipbob.zipbob_backend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
/*
비밀번호 재설정 요청
{
  "userId": "zipbob_test",
  "email": "zipbob",
  "emailAdr": "gmail.com",
  "verificationToken": "인증번호 확인 후 발급받은 토큰",
  "userPw": "Newpass1234",
  "userPwConfirm": "Newpass1234"
}
*/
public class ResetPasswordRequestDto {

    // 비밀번호를 변경할 회원 아이디
    private String userId;
    // 이메일 @ 앞부분
    private String email;
    // 이메일 도메인
    private String emailAdr;
    // 이메일 인증 완료 후 발급받은 인증 증명
    private String verificationToken;
    // 새 비밀번호
    private String userPw;
    // 새 비밀번호 확인
    private String userPwConfirm;
}