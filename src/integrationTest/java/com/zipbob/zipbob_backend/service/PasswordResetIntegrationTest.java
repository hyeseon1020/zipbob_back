package com.zipbob.zipbob_backend.service;

import com.zipbob.zipbob_backend.dto.ResetPasswordRequestDto;
import com.zipbob.zipbob_backend.entity.EmailVerification;
import com.zipbob.zipbob_backend.repository.EmailVerificationRepository;
import com.zipbob.zipbob_backend.security.JwtTokenProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("integration")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({UserService.class, EmailVerificationService.class,
        PasswordResetIntegrationTest.PasswordConfig.class})
// 비밀번호 재설정·롤백 테스트
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class PasswordResetIntegrationTest {
    private static final String OLD_PASSWORD = "Oldpass1234";
    private static final String NEW_PASSWORD = "Newpass5678";
    private static final String DOMAIN = "example.com";

    @Autowired private UserService userService;
    @Autowired private EmailVerificationRepository verificationRepository;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private PasswordEncoder passwordEncoder;

    // 재설정에서 사용하지 않는 로그인·메일 의존성만 대체한다.
    @MockitoBean private JwtTokenProvider jwtTokenProvider;
    @MockitoBean private RefreshTokenService refreshTokenService;
    @MockitoBean private EmailSendService emailSendService;

    private String userA;
    private String userB;
    private String token;
    private String oldHash;
    private Long verificationId;

    @BeforeEach
    void setUp() throws Exception {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        userA = "it_reset_a_" + suffix;
        userB = "it_reset_b_" + suffix;
        oldHash = passwordEncoder.encode(OLD_PASSWORD);
        insertUser(userA);
        insertUser(userB);
        insertRefreshToken(userA);
        insertRefreshToken(userA);
        insertRefreshToken(userB);

        // 인증번호 확인이 완료된 상태를 준비한다.
        token = Base64.getUrlEncoder().withoutPadding().encodeToString(
                MessageDigest.getInstance("SHA-256")
                        .digest(suffix.getBytes(StandardCharsets.UTF_8)));
        String hash = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                .digest(token.getBytes(StandardCharsets.UTF_8)));
        LocalDateTime now = LocalDateTime.now();
        EmailVerification verification = new EmailVerification(
                userA, DOMAIN, "RESET_PASSWORD", passwordEncoder.encode("012345"),
                now.plusMinutes(5), now);
        verification.markVerified(hash, now.plusMinutes(10), now);
        verificationId = verificationRepository.saveAndFlush(verification)
                .getEmailVerificationId();
    }

    @AfterEach
    void tearDown() {
        if (verificationId != null) {
            jdbcTemplate.update("DELETE FROM zipbob_app.email_verification_mng WHERE email_verification_id = ?",
                    verificationId);
        }
        jdbcTemplate.update("DELETE FROM zipbob_app.user_refresh_token_mng WHERE user_id IN (?, ?)", userA, userB);
        jdbcTemplate.update("DELETE FROM zipbob_app.user_mng WHERE user_id IN (?, ?)", userA, userB);
    }

    @Test
    @DisplayName("재설정 성공 시 비밀번호·인증 사용 상태를 저장하고 해당 회원의 모든 토큰을 삭제한다")
    void resetPassword_commitsChanges() {
        userService.resetPassword(request(userA, NEW_PASSWORD));
        assertSuccess();
    }

    @Test
    @DisplayName("회원 이메일 불일치로 실패하면 인증 사용 처리가 롤백되고 올바른 요청으로 재시도할 수 있다")
    void mismatchedUser_rollsBackAndAllowsRetry() {
        // A의 인증 증명은 통과하지만 B의 회원 정보와 대조하는 단계에서 실패한다.
        assertRejected(request(userB, NEW_PASSWORD), "일치하는 회원 정보가 없습니다.");
        assertUnchanged();
        userService.resetPassword(request(userA, NEW_PASSWORD));
        assertSuccess();
    }

    @Test
    @DisplayName("존재하지 않는 회원이면 인증 사용 처리도 롤백된다")
    void missingUser_rollsBack() {
        assertRejected(request("missing_" + UUID.randomUUID(), NEW_PASSWORD),
                "일치하는 회원 정보가 없습니다.");
        assertUnchanged();
    }

    @Test
    @DisplayName("사용한 인증 증명으로 비밀번호를 다시 변경할 수 없다")
    void usedProof_isRejected() {
        userService.resetPassword(request(userA, NEW_PASSWORD));
        String firstHash = passwordHash(userA);
        LocalDateTime firstUsed = verificationRepository.findById(verificationId)
                .orElseThrow().getUsedDt();
        assertRejected(request(userA, "Anotherpass9012"), "이미 사용한 이메일 인증 증명입니다.");
        assertThat(passwordHash(userA)).isEqualTo(firstHash);
        assertThat(verificationRepository.findById(verificationId).orElseThrow().getUsedDt())
                .isEqualTo(firstUsed);
        assertSuccess();
    }

    @Test
    @DisplayName("회원가입 목적의 인증 증명으로 비밀번호를 재설정할 수 없다")
    void wrongPurpose_isRejected() {
        jdbcTemplate.update("UPDATE zipbob_app.email_verification_mng SET purpose = 'SIGNUP' WHERE email_verification_id = ?",
                verificationId);
        assertRejected(request(userA, NEW_PASSWORD), "요청한 작업에 맞는 이메일 인증이 필요합니다.");
        assertUnchanged();
    }

    @Test
    @DisplayName("만료된 인증 증명은 비밀번호 재설정에 사용할 수 없다")
    void expiredProof_isRejected() {
        jdbcTemplate.update("UPDATE zipbob_app.email_verification_mng SET token_expires_dt = ? WHERE email_verification_id = ?",
                LocalDateTime.now().minusMinutes(1), verificationId);
        assertRejected(request(userA, NEW_PASSWORD), "이메일 인증 증명이 만료되었습니다.");
        assertUnchanged();
    }

    private void assertSuccess() {
        String hash = passwordHash(userA);
        assertThat(hash).isNotEqualTo(NEW_PASSWORD);
        assertThat(passwordEncoder.matches(NEW_PASSWORD, hash)).isTrue();
        assertThat(passwordEncoder.matches(OLD_PASSWORD, hash)).isFalse();
        assertThat(verificationRepository.findById(verificationId).orElseThrow().getUsedDt()).isNotNull();
        assertThat(tokenCount(userA)).isZero();
        assertThat(tokenCount(userB)).isEqualTo(1);
        assertThat(passwordHash(userB)).isEqualTo(oldHash);
    }

    private void assertUnchanged() {
        assertThat(passwordHash(userA)).isEqualTo(oldHash);
        assertThat(passwordHash(userB)).isEqualTo(oldHash);
        assertThat(tokenCount(userA)).isEqualTo(2);
        assertThat(tokenCount(userB)).isEqualTo(1);
        assertThat(verificationRepository.findById(verificationId).orElseThrow().getUsedDt()).isNull();
    }

    private void assertRejected(ResetPasswordRequestDto request, String reason) {
        assertThatThrownBy(() -> userService.resetPassword(request))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.getReason()).contains(reason);
                });
    }

    private ResetPasswordRequestDto request(String userId, String password) {
        ResetPasswordRequestDto request = new ResetPasswordRequestDto();
        ReflectionTestUtils.setField(request, "userId", userId);
        ReflectionTestUtils.setField(request, "email", userA);
        ReflectionTestUtils.setField(request, "emailAdr", DOMAIN);
        ReflectionTestUtils.setField(request, "verificationToken", token);
        ReflectionTestUtils.setField(request, "userPw", password);
        ReflectionTestUtils.setField(request, "userPwConfirm", password);
        return request;
    }

    private void insertUser(String userId) {
        jdbcTemplate.update("""
                INSERT INTO zipbob_app.user_mng
                    (user_id, user_pw, user_name, email, email_adr, user_role, created_id)
                VALUES (?, ?, ?, ?, ?, 'USER', ?)
                """, userId, oldHash, userId, userId, DOMAIN, userId);
    }

    private void insertRefreshToken(String userId) {
        String hash = UUID.randomUUID().toString().replace("-", "").repeat(2);
        jdbcTemplate.update("""
                INSERT INTO zipbob_app.user_refresh_token_mng
                    (user_id, token_hash, created_dt, expires_dt)
                VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '14 days')
                """, userId, hash);
    }

    private String passwordHash(String userId) {
        return jdbcTemplate.queryForObject("SELECT user_pw FROM zipbob_app.user_mng WHERE user_id = ?",
                String.class, userId);
    }

    private Integer tokenCount(String userId) {
        return jdbcTemplate.queryForObject("SELECT COUNT(*) FROM zipbob_app.user_refresh_token_mng WHERE user_id = ?",
                Integer.class, userId);
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class PasswordConfig {
        @Bean
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }
    }
}
