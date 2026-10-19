package com.zipbob.zipbob_backend.controller;

import com.zipbob.zipbob_backend.dto.FindIdRequestDto;
import com.zipbob.zipbob_backend.dto.ResetPasswordRequestDto;
import com.zipbob.zipbob_backend.dto.UserLoginRequestDto;
import com.zipbob.zipbob_backend.dto.UserLoginResponseDto;
import com.zipbob.zipbob_backend.dto.UserSignupRequestDto;
import com.zipbob.zipbob_backend.dto.UserSignupResponseDto;
import com.zipbob.zipbob_backend.dto.RefreshTokenRequestDto;
import com.zipbob.zipbob_backend.dto.TokenRefreshResponseDto;
import com.zipbob.zipbob_backend.dto.UserMeResponseDto;
import com.zipbob.zipbob_backend.dto.UserNicknameUpdateRequestDto;

import com.zipbob.zipbob_backend.service.RefreshTokenService;
import com.zipbob.zipbob_backend.service.UserService;
import com.zipbob.zipbob_backend.service.UserNicknameService;

import lombok.RequiredArgsConstructor;

import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.sql.SQLException;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final RefreshTokenService refreshTokenService;
    private final UserNicknameService userNicknameService;

    // 회원가입
    @PostMapping("/signup")
    public ResponseEntity<UserSignupResponseDto> signup(
            @RequestBody UserSignupRequestDto request
    ) {
        UserSignupResponseDto response =
                userService.signup(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // 로그인
    @PostMapping("/login")
    public ResponseEntity<UserLoginResponseDto> login(
            @RequestBody UserLoginRequestDto request
    ) {
        UserLoginResponseDto response =
                userService.login(request);

        return ResponseEntity.ok()
                .header("Cache-Control", "no-store")
                .header("Pragma", "no-cache")
                .body(response);
    }

    // 토큰 재발급
    @PostMapping("/refresh")
    public ResponseEntity<TokenRefreshResponseDto> refresh(
            @RequestBody RefreshTokenRequestDto request
    ) {
        TokenRefreshResponseDto response =
                refreshTokenService.refresh(request);

        return ResponseEntity.ok()
                .header("Cache-Control", "no-store")
                .header("Pragma", "no-cache")
                .body(response);
    }

    // 로그아웃
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestBody RefreshTokenRequestDto request
    ) {
        refreshTokenService.logout(request);

        return ResponseEntity.noContent()
                .header("Cache-Control", "no-store")
                .build();
    }

    // 현재 로그인한 회원 정보
    @GetMapping("/me")
    public ResponseEntity<UserMeResponseDto> me(
            Authentication authentication
    ) {
        String userId = authentication.getName();

        return ResponseEntity.ok()
                .header("Cache-Control", "no-store")
                .body(userService.getMe(userId));
    }

    // 현재 로그인한 회원의 닉네임 변경
    @PutMapping("/me/nickname")
    public ResponseEntity<UserMeResponseDto> updateNickname(
            Authentication authentication,
            @RequestBody UserNicknameUpdateRequestDto request
    ) {
        // 액세스 토큰으로 확인한 로그인 아이디
        String userId = authentication.getName();

        UserMeResponseDto response =
                userNicknameService.updateNickname(
                        userId,
                        request
                );

        return ResponseEntity.ok()
                .header("Cache-Control", "no-store")
                .body(response);
    }

    // 아이디 찾기
    @PostMapping("/find-id")
    public ResponseEntity<Map<String, String>> findId(
            @RequestBody FindIdRequestDto request
    ) {
        String userId = userService.findId(request);

        return ResponseEntity.ok()
                .header("Cache-Control", "no-store")
                .body(Map.of(
                        "userId",
                        userId
                ));
    }

    // 비밀번호 재설정
    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @RequestBody ResetPasswordRequestDto request
    ) {
        userService.resetPassword(request);

        return ResponseEntity.ok()
                .header("Cache-Control", "no-store")
                .body(Map.of(
                        "message",
                        "비밀번호가 변경되었습니다. 새 비밀번호로 로그인해 주세요."
                ));
    }

    // 사용자 API에서 발생한 요청 오류 처리
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleUserError(
            ResponseStatusException exception
    ) {
        String message = exception.getReason() == null
                ? "요청을 처리할 수 없습니다."
                : exception.getReason();

        return ResponseEntity
                .status(exception.getStatusCode())
                .header("Cache-Control", "no-store")
                .body(Map.<String, Object>of(
                        "status",
                        exception.getStatusCode().value(),
                        "message",
                        message
                ));
    }

    // 사용자 API에서 발생한 DB UNIQUE 충돌 처리
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<Map<String, Object>> handleIntegrityError(
            DataIntegrityViolationException exception
    ) {
        Throwable cause = exception;

        while (cause != null) {
            // PostgreSQL SQLState 23505: UNIQUE 조건 위반
            if (cause instanceof SQLException sqlException
                    && "23505".equals(sqlException.getSQLState())) {
                return ResponseEntity
                        .status(HttpStatus.CONFLICT)
                        .header("Cache-Control", "no-store")
                        .body(Map.<String, Object>of(
                                "status",
                                HttpStatus.CONFLICT.value(),
                                "message",
                                "입력한 정보가 이미 사용 중입니다. 중복 여부를 확인해 주세요."
                        ));
            }

            cause = cause.getCause();
        }

        // UNIQUE 충돌 이외의 DB 오류는 그대로 전달
        throw exception;
    }
}