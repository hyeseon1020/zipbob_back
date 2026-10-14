package com.zipbob.zipbob_backend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
/*
로그인 요청
{
  "userId": "dongin97",
  "userPw": "Zipbob123!"
}
*/
public class UserLoginRequestDto {

    private String userId;
    private String userPw;
}