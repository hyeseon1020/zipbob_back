package com.zipbob.zipbob_backend.service;

import com.zipbob.zipbob_backend.dto.RefreshTokenRequestDto;
import com.zipbob.zipbob_backend.dto.TokenRefreshResponseDto;
import com.zipbob.zipbob_backend.entity.User;
import com.zipbob.zipbob_backend.entity.UserRefreshToken;
import com.zipbob.zipbob_backend.repository.UserRefreshTokenRepository;
import com.zipbob.zipbob_backend.security.JwtTokenProvider;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.HexFormat;
import java.util.regex.Pattern;

@Service
public class RefreshTokenService {

    private final UserRefreshTokenRepository refreshTokenRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final long expirationDays;

    private final SecureRandom secureRandom = new SecureRandom();

    // 32바이트 난수를 Base64 URL 형식으로 인코딩하면 43자
    private static final Pattern REFRESH_TOKEN_PATTERN =
            Pattern.compile("[A-Za-z0-9_-]{43}");

    public RefreshTokenService(
            UserRefreshTokenRepository refreshTokenRepository,
            JwtTokenProvider jwtTokenProvider,
            @Value("${jwt.refresh-token-expiration-days}")
            long expirationDays
    ) {
        if (expirationDays <= 0) {
            throw new IllegalArgumentException(
                    "리프레시 토큰 유효기간은 0보다 커야 합니다."
            );
        }

        this.refreshTokenRepository = refreshTokenRepository;
        this.jwtTokenProvider = jwtTokenProvider;
        this.expirationDays = expirationDays;
    }

    // 리프레시 토큰 생성 및 해시 저장
    @Transactional
    public String issue(User user) {

        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);

        String rawToken = Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(randomBytes);

        String tokenHash = hashToken(rawToken);

        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        UserRefreshToken refreshToken = new UserRefreshToken(
                user,
                tokenHash,
                now,
                now.plusDays(expirationDays)
        );

        refreshTokenRepository.save(refreshToken);

        return rawToken;
    }

    // 토큰 재발급
    @Transactional
    public TokenRefreshResponseDto refresh(
            RefreshTokenRequestDto request
    ) {
        // 1. 입력 확인 및 해시 계산
        String rawToken = validateRequest(request);
        String tokenHash = hashToken(rawToken);

        // 2. 토큰 조회 및 행 잠금
        UserRefreshToken storedToken =
                refreshTokenRepository
                        .findByTokenHashForUpdate(tokenHash)
                        .orElseThrow(RefreshTokenService::invalidToken);

        // 3. 만료 여부 확인
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        if (storedToken.isExpired(now)) {
            throw invalidToken();
        }

        User user = storedToken.getUser();

        // 4. 새 액세스 토큰 생성
        String newAccessToken =
                jwtTokenProvider.createAccessToken(user.getUserId());

        // 5. 이전 리프레시 토큰 삭제
        refreshTokenRepository.delete(storedToken);
        refreshTokenRepository.flush();

        // 6. 새 리프레시 토큰 발급 및 해시 저장
        String newRefreshToken = issue(user);

        // 7. 새 토큰 두 개 반환
        return new TokenRefreshResponseDto(
                newAccessToken,
                newRefreshToken,
                jwtTokenProvider.getAccessTokenExpirationSeconds()
        );
    }

    // 현재 로그인에 해당하는 리프레시 토큰 삭제
    @Transactional
    public void logout(RefreshTokenRequestDto request) {

        String rawToken = validateRequest(request);
        String tokenHash = hashToken(rawToken);

        // 이미 삭제된 토큰이면 그대로 종료
        refreshTokenRepository
                .findByTokenHashForUpdate(tokenHash)
                .ifPresent(refreshTokenRepository::delete);
    }

    // SHA-256 해시 계산
    public String hashToken(String rawToken) {

        if (rawToken == null || rawToken.isBlank()) {
            throw new IllegalArgumentException(
                    "리프레시 토큰이 필요합니다."
            );
        }

        try {
            MessageDigest digest =
                    MessageDigest.getInstance("SHA-256");

            byte[] hashedBytes = digest.digest(
                    rawToken.getBytes(StandardCharsets.UTF_8)
            );

            return HexFormat.of().formatHex(hashedBytes);

        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "SHA-256 해시를 생성할 수 없습니다.",
                    exception
            );
        }
    }

    // 재발급·로그아웃 요청 검증
    private static String validateRequest(
            RefreshTokenRequestDto request
    ) {
        if (request == null
                || request.getRefreshToken() == null
                || request.getRefreshToken().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "리프레시 토큰이 필요합니다."
            );
        }

        String rawToken = request.getRefreshToken();

        // 토큰은 공백을 제거하거나 값을 변경하지 않음
        if (!REFRESH_TOKEN_PATTERN.matcher(rawToken).matches()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "리프레시 토큰 형식이 올바르지 않습니다."
            );
        }

        return rawToken;
    }

    private static ResponseStatusException invalidToken() {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "유효하지 않거나 만료된 리프레시 토큰입니다. 다시 로그인해 주세요."
        );
    }
}