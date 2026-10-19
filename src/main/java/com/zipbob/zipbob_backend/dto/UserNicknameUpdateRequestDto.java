package com.zipbob.zipbob_backend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
/*
닉네임 변경
{
  "userName": "집밥요리사"
}
*/
public class UserNicknameUpdateRequestDto {

    private String userName;
}