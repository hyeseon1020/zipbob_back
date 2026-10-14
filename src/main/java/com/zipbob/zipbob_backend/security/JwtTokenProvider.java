package com.zipbob.zipbob_backend.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;

@Component
public class JwtTokenProvider {

    private final SecretKey secretKey;
    private final JwtParser jwtParser;
    private final String issuer;
    private final long accessTokenExpirationSeconds;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.issuer}") String issuer,
            @Value("${jwt.access-token-expiration-seconds}")
            long accessTokenExpirationSeconds
    ) {
        byte[] keyBytes = Decoders.BASE64.decode(secret);

        // 이번 구현에서는 HS256용 32바이트 랜덤 키 사용
        if (keyBytes.length != 32) {
            throw new IllegalArgumentException(
                    "JWT_SECRET은 32바이트 랜덤 키를 Base64로 인코딩해야 합니다."
            );
        }

        if (issuer == null || issuer.isBlank()) {
            throw new IllegalArgumentException(
                    "JWT 발급자 설정이 필요합니다."
            );
        }

        if (accessTokenExpirationSeconds <= 0) {
            throw new IllegalArgumentException(
                    "액세스 토큰 유효기간은 0보다 커야 합니다."
            );
        }

        this.secretKey = Keys.hmacShaKeyFor(keyBytes);
        this.issuer = issuer;
        this.accessTokenExpirationSeconds =
                accessTokenExpirationSeconds;

        // 검증기: 서명·발급자·토큰 종류 확인
        this.jwtParser = Jwts.parser()
                .verifyWith(secretKey)
                .requireIssuer(issuer)
                // 액세스 토큰인지 확인
                .require("tokenType", "access")
                .build();
    }

    // 액세스 토큰 생성
    public String createAccessToken(String userId) {

        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException(
                    "토큰을 발급할 회원 아이디가 필요합니다."
            );
        }

        Instant now = Instant.now();
        Instant expiresAt =
                now.plusSeconds(accessTokenExpirationSeconds);

        return Jwts.builder()
                .issuer(issuer)
                // 토큰에 회원 아이디 저장
                .subject(userId)
                .id(UUID.randomUUID().toString())
                .claim("tokenType", "access")
                // 발급 시간
                .issuedAt(Date.from(now))
                // 만료 시간
                .expiration(Date.from(expiresAt))
                // 서버 비밀키로 서명
                .signWith(secretKey, Jwts.SIG.HS256)
                .compact();
    }

    /*
     토큰 검증 후 회원 아이디 반환.
     서명 오류·만료·잘못된 형식은 예외 발생.
     */
    public String validateAndGetUserId(String accessToken) {

        if (accessToken == null || accessToken.isBlank()) {
            throw new JwtException("액세스 토큰이 없습니다.");
        }

        // 서명과 만료 시간은 파싱 과정에서 검증
        Claims claims = jwtParser
                // 서명·만료 검증 후 내용 읽기
                .parseSignedClaims(accessToken)
                .getPayload();

        // 만료 시간이 없는 토큰도 거부
        if (claims.getExpiration() == null) {
            throw new JwtException(
                    "토큰에 만료 시간이 없습니다."
            );
        }

        String userId = claims.getSubject();

        if (userId == null || userId.isBlank()) {
            throw new JwtException(
                    "토큰에 회원 아이디가 없습니다."
            );
        }

        return userId;
    }

    // 로그인 응답에서 유효기간 안내에 사용
    public long getAccessTokenExpirationSeconds() {
        return accessTokenExpirationSeconds;
    }
}