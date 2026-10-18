package com.zipbob.zipbob_backend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
/*
회원가입 요청 DTO (프론트 -> 백엔드)
{
  "userId": "testuser",
  "userPw": "Zipbob123!",
  "userPwConfirm": "Zipbob123!",
  "userName": "집밥테스터",
  "email": "test",
  "emailAdr": "gmail.com",
  "userDeviceId": 5,
  "verificationToken": "인증번호 확인 API에서 받은 verificationToken"
}
*/
public class UserSignupRequestDto {

    private String userId;
    private String userPw;
    // 비밀번호 확인용 — DB 저장 안 함
    private String userPwConfirm;
    // 닉네임
    private String userName;
    // 이메일 @ 앞부분
    private String email;
    // 이메일 도메인
    private String emailAdr;
    // 앱 시작 시 등록한 기기 ID
    private Integer userDeviceId;
    // 이메일 인증 성공 후 받은 증명 토큰
    private String verificationToken;
}