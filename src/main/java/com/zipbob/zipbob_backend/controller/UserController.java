package com.zipbob.zipbob_backend.controller;

import com.zipbob.zipbob_backend.dto.UserSignupRequestDto;
import com.zipbob.zipbob_backend.dto.UserSignupResponseDto;
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

    // 가입 입력 오류·중복 오류를 화면에 전달
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleSignupError(
            ResponseStatusException exception
    ) {
        String message = exception.getReason() == null
                ? "요청을 처리할 수 없습니다."
                : exception.getReason();

        return ResponseEntity
                .status(exception.getStatusCode())
                .body(Map.<String, Object>of(
                        "status", exception.getStatusCode().value(),
                        "message", message
                ));
    }
}