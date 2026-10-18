package com.zipbob.zipbob_backend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
// 이메일 인증관리 (email_verification_mng)
@Entity
@Table(name = "email_verification_mng", schema = "zipbob_app")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailVerification {

    // 이메일 인증 요청 고유 번호 — DB에서 자동 생성
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "email_verification_id")
    private Long emailVerificationId;

    // 이메일 @ 앞부분
    @Column(name = "email", nullable = false, length = 50)
    private String email;

    // 이메일 @ 뒷부분
    @Column(name = "email_adr", nullable = false, length = 50)
    private String emailAdr;

    // SIGNUP / FIND_ID / RESET_PASSWORD
    @Column(name = "purpose", nullable = false, length = 30)
    private String purpose;

    // 인증번호 원문이 아닌 해시값 저장
    @Column(name = "verification_code_hash", nullable = false, length = 255)
    private String verificationCodeHash;

    // 인증번호 만료 시각
    @Column(name = "expires_dt", nullable = false)
    private LocalDateTime expiresDt;

    // 인증번호 확인 성공 시각
    @Column(name = "verified_dt")
    private LocalDateTime verifiedDt;

    // 인증 성공 후 발급하는 증명 토큰의 해시값
    @Column(name = "verification_token_hash", unique = true, length = 255)
    private String verificationTokenHash;

    // 인증 증명 토큰 만료 시각
    @Column(name = "token_expires_dt")
    private LocalDateTime tokenExpiresDt;

    // 회원가입 등 실제 작업에 인증 증명을 사용한 시각
    @Column(name = "used_dt")
    private LocalDateTime usedDt;

    // 인증번호 입력 실패 횟수
    @Column(name = "attempt_count", nullable = false)
    private int attemptCount;

    // 인증 요청 생성 시각
    @Column(name = "created_dt", nullable = false, updatable = false)
    private LocalDateTime createdDt;

    // 새 인증 요청 생성
    // 해시 생성 및 만료 시각 계산은 Service에서 처리
    public EmailVerification(
            String email,
            String emailAdr,
            String purpose,
            String verificationCodeHash,
            LocalDateTime expiresDt,
            LocalDateTime createdDt
    ) {
        this.email = email;
        this.emailAdr = emailAdr;
        this.purpose = purpose;
        this.verificationCodeHash = verificationCodeHash;
        this.expiresDt = expiresDt;
        this.createdDt = createdDt;
        this.attemptCount = 0;
    }

    // 인증번호 불일치 시 실패 횟수 증가
    public void increaseAttemptCount() {
        this.attemptCount++;
    }

    // 인증번호 확인 성공 후 인증 증명 정보 저장
    public void markVerified(
            String verificationTokenHash,
            LocalDateTime tokenExpiresDt,
            LocalDateTime verifiedDt
    ) {
        this.verificationTokenHash = verificationTokenHash;
        this.tokenExpiresDt = tokenExpiresDt;
        this.verifiedDt = verifiedDt;
    }

    // 회원가입 등 실제 작업에 인증 증명 사용 완료
    public void markUsed(LocalDateTime usedDt) {
        this.usedDt = usedDt;
    }
}