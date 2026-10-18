package com.zipbob.zipbob_backend.controller;

import com.zipbob.zipbob_backend.dto.UserLoginRequestDto;
import com.zipbob.zipbob_backend.dto.UserLoginResponseDto;
import com.zipbob.zipbob_backend.dto.UserSignupRequestDto;
import com.zipbob.zipbob_backend.dto.UserSignupResponseDto;
import com.zipbob.zipbob_backend.dto.RefreshTokenRequestDto;
import com.zipbob.zipbob_backend.dto.TokenRefreshResponseDto;
import com.zipbob.zipbob_backend.dto.UserMeResponseDto;
import org.springframework.security.core.Authentication;
import com.zipbob.zipbob_backend.service.RefreshTokenService;
import com.zipbob.zipbob_backend.service.UserService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final RefreshTokenService refreshTokenService;

    // 회원가입
    @PostMapping("/signup")
    public ResponseEntity<UserSignupResponseDto> signup(
            @RequestBody UserSignupRequestDto request
    ) {
        UserSignupResponseDto response = userService.signup(request);

        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(response);
    }

    // 로그인 정보 검증
    @PostMapping("/login")
    public ResponseEntity<UserLoginResponseDto> login(
            @RequestBody UserLoginRequestDto  request
    ){
        UserLoginResponseDto response = userService.login(request);

        return ResponseEntity.ok()
                .header("Cache-Control", "no-store")
                .header("Pragma", "no-cache")
                .body(response);
    }

    // 회원가입·로그인 등 사용자 API의 요청 오류 처리
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
                        "status", exception.getStatusCode().value(),
                        "message", message
                ));
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
        // 요청 파라미터가 아닌, 검증된 토큰의 회원 아이디 사용
        String userId = authentication.getName();

        return ResponseEntity.ok()
                .header("Cache-Control", "no-store")
                .body(userService.getMe(userId));
    }
}