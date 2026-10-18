package com.zipbob.zipbob_backend.controller;

import com.zipbob.zipbob_backend.dto.EmailVerificationConfirmRequestDto;
import com.zipbob.zipbob_backend.dto.EmailVerificationConfirmResponseDto;
import com.zipbob.zipbob_backend.dto.EmailVerificationSendRequestDto;
import com.zipbob.zipbob_backend.dto.EmailVerificationSendResponseDto;
import com.zipbob.zipbob_backend.service.EmailVerificationService;

import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/email-verifications")
@RequiredArgsConstructor
// 회원가입_이메일 인증
public class EmailVerificationController {

    private final EmailVerificationService emailVerificationService;

    // 인증번호 발송
    @PostMapping("/send")
    public ResponseEntity<EmailVerificationSendResponseDto> send(
            @RequestBody EmailVerificationSendRequestDto request
    ) {
        EmailVerificationSendResponseDto response =
                emailVerificationService.send(request);

        return ResponseEntity.ok()
                .header("Cache-Control", "no-store")
                .body(response);
    }

    // 인증번호 확인
    @PostMapping("/confirm")
    public ResponseEntity<EmailVerificationConfirmResponseDto> confirm(
            @RequestBody EmailVerificationConfirmRequestDto request
    ) {
        EmailVerificationConfirmResponseDto response =
                emailVerificationService.confirm(request);

        return ResponseEntity.ok()
                .header("Cache-Control", "no-store")
                .body(response);
    }

    // Service에서 발생한 요청 오류를 JSON으로 응답
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, Object>> handleVerificationError(
            ResponseStatusException exception
    ) {
        String message = exception.getReason() == null
                ? "이메일 인증 요청을 처리할 수 없습니다."
                : exception.getReason();

        return ResponseEntity
                .status(exception.getStatusCode())
                .header("Cache-Control", "no-store")
                .body(Map.<String, Object>of(
                        "status", exception.getStatusCode().value(),
                        "message", message
                ));
    }
}