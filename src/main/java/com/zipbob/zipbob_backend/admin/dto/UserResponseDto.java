package com.zipbob.zipbob_backend.admin.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserResponseDto {
    private String userId;       //아이디
    private Integer userDevice;    // 기기아이디
    private String userName;   // 회원명
    private String email;      // 이메일
    private String emailAdr;    // 도메인
    private String userRole;    //규정
}