package com.zipbob.zipbob_backend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
/*
토큰 재발급과 로그아웃
{
  "refreshToken": "로그인 응답으로 받은 리프레시 토큰"
}
*/
public class RefreshTokenRequestDto {

    private String refreshToken;
}