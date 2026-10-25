package com.zipbob.zipbob_backend.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
// JWT 자동 테스트
class JwtTokenProviderTest {

    // 테스트 전용 키: Base64 디코딩 결과 32바이트
    private static final String TEST_SECRET =
            "MDEyMzQ1Njc4OWFiY2RlZjAxMjM0NTY3ODlhYmNkZWY=";

    private static final String ISSUER = "zipbob-pick";
    private static final long EXPIRATION_SECONDS = 1800;

    private JwtTokenProvider jwtTokenProvider;

    @BeforeEach
    void setUp() {
        jwtTokenProvider = new JwtTokenProvider(
                TEST_SECRET,
                ISSUER,
                EXPIRATION_SECONDS
        );
    }

    @Test
    @DisplayName("정상 토큰을 검증하면 회원 아이디를 반환한다")
    void validTokenReturnsUserId() {
        // 준비
        String userId = "test-user";

        // 실행
        String token = jwtTokenProvider.createAccessToken(userId);
        String result = jwtTokenProvider.validateAndGetUserId(token);

        // 검증
        assertEquals(userId, result);
    }

    @Test
    @DisplayName("다른 키로 서명한 토큰은 거부한다")
    void rejectsTokenSignedWithDifferentKey() {
        // 준비: 원래 키에서 한 바이트를 바꿔 다른 키 생성
        byte[] otherKeyBytes = Decoders.BASE64.decode(TEST_SECRET);
        otherKeyBytes[0] ^= 1;
        SecretKey otherKey = Keys.hmacShaKeyFor(otherKeyBytes);

        Instant now = Instant.now();

        String token = Jwts.builder()
                .issuer(ISSUER)
                .subject("test-user")
                .claim("tokenType", "access")
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plusSeconds(EXPIRATION_SECONDS)))
                .signWith(otherKey, Jwts.SIG.HS256)
                .compact();

        // 실행 및 검증
        assertThrows(
                SignatureException.class,
                () -> jwtTokenProvider.validateAndGetUserId(token)
        );
    }

    @Test
    @DisplayName("만료된 토큰은 거부한다")
    void rejectsExpiredToken() {
        // 준비: 올바른 키로 이미 만료된 토큰 생성
        SecretKey key = Keys.hmacShaKeyFor(
                Decoders.BASE64.decode(TEST_SECRET)
        );

        Instant now = Instant.now();

        String token = Jwts.builder()
                .issuer(ISSUER)
                .subject("test-user")
                .claim("tokenType", "access")
                .issuedAt(Date.from(now.minusSeconds(120)))
                .expiration(Date.from(now.minusSeconds(60)))
                .signWith(key, Jwts.SIG.HS256)
                .compact();

        // 실행 및 검증
        assertThrows(
                ExpiredJwtException.class,
                () -> jwtTokenProvider.validateAndGetUserId(token)
        );
    }

    @Test
    @DisplayName("회원 아이디가 null 또는 공백이면 토큰 발급을 거부한다")
    void rejectsMissingUserId() {
        assertThrows(
                IllegalArgumentException.class,
                () -> jwtTokenProvider.createAccessToken(null)
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> jwtTokenProvider.createAccessToken("")
        );

        assertThrows(
                IllegalArgumentException.class,
                () -> jwtTokenProvider.createAccessToken("   ")
        );
    }
}