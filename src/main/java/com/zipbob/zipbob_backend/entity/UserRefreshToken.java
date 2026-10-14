package com.zipbob.zipbob_backend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.OffsetDateTime;

@Entity
@Table(name = "user_refresh_token_mng", schema = "zipbob_app")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserRefreshToken {

    // 토큰 관리 번호
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "refresh_token_id")
    private Long refreshTokenId;

    // 토큰을 발급받은 회원
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    // 리프레시 토큰 원문이 아닌 SHA-256 해시
    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    // 만료 일시: PostgreSQL TIMESTAMPTZ
    @JdbcTypeCode(SqlTypes.TIMESTAMP_WITH_TIMEZONE)
    @Column(name = "expires_dt", nullable = false)
    private OffsetDateTime expiresDt;

    // 발급 일시
    @JdbcTypeCode(SqlTypes.TIMESTAMP_WITH_TIMEZONE)
    @Column(name = "created_dt", nullable = false, updatable = false)
    private OffsetDateTime createdDt;

    public UserRefreshToken(
            User user,
            String tokenHash,
            OffsetDateTime createdDt,
            OffsetDateTime expiresDt
    ) {
        this.user = user;
        this.tokenHash = tokenHash;
        this.createdDt = createdDt;
        this.expiresDt = expiresDt;
    }

    // 만료 시각에 도달한 토큰도 만료로 처리
    public boolean isExpired(OffsetDateTime now) {
        return !expiresDt.isAfter(now);
    }
}