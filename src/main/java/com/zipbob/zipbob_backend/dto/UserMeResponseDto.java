package com.zipbob.zipbob_backend.dto;

import com.zipbob.zipbob_backend.entity.User;
import lombok.Getter;

@Getter
public class UserMeResponseDto {

    private final String userId;
    private final String userName;
    private final String email;

    public UserMeResponseDto(User user) {
        this.userId = user.getUserId();
        this.userName = user.getUserName();

        this.email = user.getEmail() != null
                && user.getEmailAdr() != null
                ? user.getEmail() + "@" + user.getEmailAdr()
                : null;
    }
}