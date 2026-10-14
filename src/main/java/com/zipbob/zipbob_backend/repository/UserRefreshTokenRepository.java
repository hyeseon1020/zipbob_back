package com.zipbob.zipbob_backend.repository;

import com.zipbob.zipbob_backend.entity.UserRefreshToken;

import jakarta.persistence.LockModeType;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.OffsetDateTime;
import java.util.Optional;
// 관리할 엔티티: UserRefreshToken, 타입: Long
public interface UserRefreshTokenRepository
        extends JpaRepository<UserRefreshToken, Long> {

    /*
     토큰 해시로 조회하고 해당 행을 잠금.
     같은 리프레시 토큰으로 동시에 재발급되는 것을 방지하기 위해 사용.
     save()와 delete()는 JpaRepository에서 제공
     */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT token
        FROM UserRefreshToken token
        WHERE token.tokenHash = :tokenHash
        """)
    // 재발급·로그아웃 대상 조회
    Optional<UserRefreshToken> findByTokenHashForUpdate(
            @Param("tokenHash") String tokenHash
    );

    // 특정 회원의 모든 리프레시 토큰 삭제
    @Modifying
    @Query("""
        DELETE FROM UserRefreshToken token
        WHERE token.user.userId = :userId
        """)
    // 해당 회원의 모든 기기 로그아웃
    int deleteAllByUserId(
            @Param("userId") String userId
    );

    // 만료된 리프레시 토큰 정리
    @Modifying
    @Query("""
        DELETE FROM UserRefreshToken token
        WHERE token.expiresDt <= :now
        """)
    // 만료 데이터 정리
    int deleteExpiredTokens(
            @Param("now") OffsetDateTime now
    );
}