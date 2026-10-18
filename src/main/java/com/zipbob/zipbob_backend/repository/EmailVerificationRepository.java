package com.zipbob.zipbob_backend.repository;

import com.zipbob.zipbob_backend.entity.EmailVerification;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
// 관리할 엔티티 : EmailVerification , 타입 : Long
public interface EmailVerificationRepository
        extends JpaRepository<EmailVerification, Long> {

    // 이메일·인증 목적별 가장 최근 요청 조회
    // 최근 발송 시각을 확인해 재발송 간격 제한에 사용
    Optional<EmailVerification>
    findFirstByEmailAndEmailAdrAndPurposeOrderByCreatedDtDescEmailVerificationIdDesc(
            String email,
            String emailAdr,
            String purpose
    );

    // 인증 요청 ID로 조회하고 해당 행 잠금
    // 인증번호 확인 시 실패 횟수·인증 완료 상태의 동시 변경 방지
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT ev
        FROM EmailVerification ev
        WHERE ev.emailVerificationId = :emailVerificationId
        """)
    Optional<EmailVerification> findByIdForUpdate(
            @Param("emailVerificationId") Long emailVerificationId
    );

    // 인증 증명 토큰의 해시값으로 조회하고 해당 행 잠금
    // 같은 증명을 동시에 사용하는 요청을 순서대로 처리
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT ev
        FROM EmailVerification ev
        WHERE ev.verificationTokenHash = :verificationTokenHash
        """)
    Optional<EmailVerification> findByTokenHashForUpdate(
            @Param("verificationTokenHash") String verificationTokenHash
    );
}