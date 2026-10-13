package com.zipbob.zipbob_backend.dto;

import com.zipbob.zipbob_backend.entity.User;

import lombok.Getter;

@Getter
// 회원가입 응답 DTO (백엔드 -> 프론트) : 원문 비밀번호와 비밀번호 해시는 모두 응답에서 제외
public class UserSignupResponseDto {

    private final String message;
    private final String userId;
    private final String userName;
    private final String email;

    public UserSignupResponseDto(User user) {
        this.message = "회원가입이 완료되었습니다.";
        this.userId = user.getUserId();
        this.userName = user.getUserName();
        this.email = user.getEmail() + "@" + user.getEmailAdr();
    }
}