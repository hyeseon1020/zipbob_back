package com.zipbob.zipbob_backend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
/*
회원가입 요청 DTO (프론트 -> 백엔드)
{
  "userId": "dongin97",
  "userPw": "Zipbob123!",
  "userPwConfirm": "Zipbob123!",
  "userName": "사과",
  "email": "dongin97",
  "emailAdr": "gmail.com",
  "userDeviceId": 5
}
*/
public class UserSignupRequestDto {

    private String userId;
    private String userPw;
    // 확인용으로만 사용하고 DB에는 저장하지 않음
    private String userPwConfirm;
    // 닉네임
    private String userName;
    // 이메일 @ 앞부분
    private String email;
    // 이메일 도메인
    private String emailAdr;
    // 앱 시작 시 기기 등록 API에서 받은 번호
    private Integer userDeviceId;
}