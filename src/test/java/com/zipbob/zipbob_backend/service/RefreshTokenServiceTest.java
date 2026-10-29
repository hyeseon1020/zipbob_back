package com.zipbob.zipbob_backend.service;

import com.zipbob.zipbob_backend.dto.RefreshTokenRequestDto;
import com.zipbob.zipbob_backend.dto.TokenRefreshResponseDto;
import com.zipbob.zipbob_backend.entity.User;
import com.zipbob.zipbob_backend.entity.UserRefreshToken;
import com.zipbob.zipbob_backend.repository.UserRefreshTokenRepository;
import com.zipbob.zipbob_backend.security.JwtTokenProvider;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.server.ResponseStatusException;

import java.time.OffsetDateTime;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@DataJpaTest
@ActiveProfiles("test")
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
@Import(RefreshTokenService.class)
// RefreshToken 테스트
class RefreshTokenServiceTest {

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private UserRefreshTokenRepository refreshTokenRepository;

    @Autowired
    private EntityManager entityManager;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    private User user;

    @BeforeEach
    void setUp() {
        // User의 기본 생성자는 protected이므로 테스트에서 직접 생성
        user = BeanUtils.instantiateClass(User.class);

        ReflectionTestUtils.setField(
                user, "userId", "refresh-test-user"
        );
        ReflectionTestUtils.setField(
                user, "userPw", "test-password-hash"
        );
        ReflectionTestUtils.setField(
                user, "userName", "토큰테스트회원"
        );

        entityManager.persist(user);
        entityManager.flush();
    }

    @Test
    @DisplayName("발급한 Refresh Token은 원문 대신 SHA-256 해시로 저장된다")
    void issue_storesHashInsteadOfRawToken() {
        String rawToken = refreshTokenService.issue(user);

        flushAndClear();

        String tokenHash = refreshTokenService.hashToken(rawToken);

        UserRefreshToken storedToken = refreshTokenRepository
                .findByTokenHashForUpdate(tokenHash)
                .orElseThrow();

        assertThat(rawToken).matches("[A-Za-z0-9_-]{43}");
        assertThat(storedToken.getTokenHash())
                .isEqualTo(tokenHash)
                .isNotEqualTo(rawToken)
                .hasSize(64);
        assertThat(storedToken.getUser().getUserId())
                .isEqualTo("refresh-test-user");
        assertThat(storedToken.getExpiresDt())
                .isAfter(storedToken.getCreatedDt());
    }

    @Test
    @DisplayName("재발급하면 새 토큰을 반환하고 기존 Refresh Token을 교체한다")
    void refresh_rotatesToken() {
        String oldToken = refreshTokenService.issue(user);
        prepareAccessToken();

        TokenRefreshResponseDto response =
                refreshTokenService.refresh(request(oldToken));

        flushAndClear();

        assertThat(response.getAccessToken())
                .isEqualTo("new-access-token");
        assertThat(response.getTokenType()).isEqualTo("Bearer");
        assertThat(response.getExpiresIn()).isEqualTo(1800L);
        assertThat(response.getRefreshToken())
                .isNotEqualTo(oldToken)
                .matches("[A-Za-z0-9_-]{43}");

        assertThat(refreshTokenRepository.findByTokenHashForUpdate(
                refreshTokenService.hashToken(oldToken)
        )).isEmpty();

        UserRefreshToken newStoredToken = refreshTokenRepository
                .findByTokenHashForUpdate(
                        refreshTokenService.hashToken(
                                response.getRefreshToken()
                        )
                )
                .orElseThrow();

        assertThat(newStoredToken.getUser().getUserId())
                .isEqualTo("refresh-test-user");
        assertThat(refreshTokenRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("교체된 이전 Refresh Token은 재사용할 수 없다")
    void refresh_rejectsReusedToken() {
        String oldToken = refreshTokenService.issue(user);
        prepareAccessToken();

        refreshTokenService.refresh(request(oldToken));
        flushAndClear();

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> refreshTokenService.refresh(request(oldToken))
        );

        assertThat(exception.getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    @DisplayName("교체 후 새 Refresh Token으로 다시 재발급할 수 있다")
    void refresh_acceptsRotatedToken() {
        String originalToken = refreshTokenService.issue(user);
        prepareAccessToken();

        TokenRefreshResponseDto first =
                refreshTokenService.refresh(request(originalToken));

        flushAndClear();

        TokenRefreshResponseDto second =
                refreshTokenService.refresh(
                        request(first.getRefreshToken())
                );

        flushAndClear();

        assertThat(second.getRefreshToken())
                .isNotEqualTo(originalToken)
                .isNotEqualTo(first.getRefreshToken());

        assertThat(refreshTokenRepository.findByTokenHashForUpdate(
                refreshTokenService.hashToken(first.getRefreshToken())
        )).isEmpty();

        assertThat(refreshTokenRepository.findByTokenHashForUpdate(
                refreshTokenService.hashToken(second.getRefreshToken())
        )).isPresent();

        assertThat(refreshTokenRepository.count()).isEqualTo(1);
    }

    @Test
    @DisplayName("로그아웃하면 Refresh Token이 삭제되고 재발급이 차단된다")
    void logout_deletesTokenAndRejectsRefresh() {
        String rawToken = refreshTokenService.issue(user);

        refreshTokenService.logout(request(rawToken));
        flushAndClear();

        assertThat(refreshTokenRepository.findByTokenHashForUpdate(
                refreshTokenService.hashToken(rawToken)
        )).isEmpty();

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> refreshTokenService.refresh(request(rawToken))
        );

        assertThat(exception.getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(jwtTokenProvider);
    }

    @Test
    @DisplayName("같은 Refresh Token으로 반복 로그아웃해도 오류가 발생하지 않는다")
    void logout_allowsRepeatedRequests() {
        String rawToken = refreshTokenService.issue(user);

        refreshTokenService.logout(request(rawToken));
        flushAndClear();

        refreshTokenService.logout(request(rawToken));
        flushAndClear();

        assertThat(refreshTokenRepository.count()).isZero();
        verifyNoInteractions(jwtTokenProvider);
    }

    @Test
    @DisplayName("만료된 Refresh Token으로 재발급할 수 없다")
    void refresh_rejectsExpiredToken() {
        String rawToken = "A".repeat(43);
        OffsetDateTime now = OffsetDateTime.now(ZoneOffset.UTC);

        refreshTokenRepository.saveAndFlush(
                new UserRefreshToken(
                        user,
                        refreshTokenService.hashToken(rawToken),
                        now.minusDays(15),
                        now.minusDays(1)
                )
        );
        entityManager.clear();

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> refreshTokenService.refresh(request(rawToken))
        );

        assertThat(exception.getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);
        verifyNoInteractions(jwtTokenProvider);
    }

    private void prepareAccessToken() {
        when(jwtTokenProvider.createAccessToken("refresh-test-user"))
                .thenReturn("new-access-token");
        when(jwtTokenProvider.getAccessTokenExpirationSeconds())
                .thenReturn(1800L);
    }

    private RefreshTokenRequestDto request(String rawToken) {
        RefreshTokenRequestDto request = new RefreshTokenRequestDto();
        ReflectionTestUtils.setField(request, "refreshToken", rawToken);
        return request;
    }

    private void flushAndClear() {
        entityManager.flush();
        entityManager.clear();
    }
}