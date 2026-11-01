package com.zipbob.zipbob_backend.service;

import com.zipbob.zipbob_backend.dto.EmailVerificationConfirmRequestDto;
import com.zipbob.zipbob_backend.entity.EmailVerification;
import com.zipbob.zipbob_backend.repository.EmailVerificationRepository;
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

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("integration")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({EmailVerificationService.class,
        EmailVerificationConfirmIntegrationTest.PasswordConfig.class})
// 서비스 자체의 트랜잭션이 끝난 후 커밋된 DB 상태를 검증한다.
// 이메일 인증 통합 테스트
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class EmailVerificationConfirmIntegrationTest {

    private static final String CORRECT_CODE = "012345";
    private static final String WRONG_CODE = "999999";

    @Autowired
    private EmailVerificationService verificationService;

    @Autowired
    private EmailVerificationRepository verificationRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @MockitoBean
    private EmailSendService emailSendService;

    private Long verificationId;

    @BeforeEach
    void setUp() {
        LocalDateTime now = LocalDateTime.now();
        EmailVerification verification = new EmailVerification(
                "it-" + UUID.randomUUID(),
                "example.com",
                "SIGNUP",
                passwordEncoder.encode(CORRECT_CODE),
                now.plusMinutes(5),
                now
        );

        // Repository 트랜잭션에서 저장을 완료한 뒤 서비스를 호출한다.
        verificationId = verificationRepository.saveAndFlush(verification)
                .getEmailVerificationId();
    }

    @AfterEach
    void tearDown() {
        // 테스트가 커밋한 데이터 중 이번 테스트가 생성한 행만 정리한다.
        if (verificationId != null) {
            jdbcTemplate.update("""
                    DELETE FROM zipbob_app.email_verification_mng
                    WHERE email_verification_id = ?
                    """, verificationId);
        }
    }

    @Test
    @DisplayName("인증번호가 틀려 예외가 발생해도 실패 횟수는 DB에 저장된다")
    void wrongCode_commitsAttemptCount() {
        assertRejected(WRONG_CODE, "인증번호가 일치하지 않습니다.");

        assertThat(attemptCount()).isEqualTo(1);
        assertNotVerified();
    }

    @Test
    @DisplayName("5회 실패 후에는 올바른 인증번호도 차단한다")
    void fiveFailures_blockCorrectCode() {
        for (int attempt = 1; attempt <= 5; attempt++) {
            String message = attempt == 5
                    ? "인증 시도 횟수를 초과했습니다."
                    : "인증번호가 일치하지 않습니다.";

            assertRejected(WRONG_CODE, message);
            assertThat(attemptCount()).isEqualTo(attempt);
        }

        assertRejected(CORRECT_CODE, "인증 시도 횟수를 초과했습니다.");
        assertThat(attemptCount()).isEqualTo(5);
        assertNotVerified();
    }

    @Test
    @DisplayName("4회 실패 후 올바른 인증번호를 입력하면 인증에 성공한다")
    void fourFailures_allowCorrectCode() {
        for (int attempt = 1; attempt <= 4; attempt++) {
            assertRejected(WRONG_CODE, "인증번호가 일치하지 않습니다.");
        }

        assertThat(verificationService.confirm(request(CORRECT_CODE)))
                .isNotNull();

        EmailVerification saved = savedVerification();
        assertThat(saved.getAttemptCount()).isEqualTo(4);
        assertThat(saved.getVerifiedDt()).isNotNull();
        assertThat(saved.getVerificationTokenHash()).matches("[0-9a-f]{64}");
        assertThat(saved.getTokenExpiresDt()).isAfter(saved.getVerifiedDt());
        assertThat(saved.getUsedDt()).isNull();
    }

    @Test
    @DisplayName("만료된 인증번호는 올바른 번호여도 거절한다")
    void expiredCode_isRejected() {
        jdbcTemplate.update("""
                UPDATE zipbob_app.email_verification_mng
                SET expires_dt = ?
                WHERE email_verification_id = ?
                """, LocalDateTime.now().minusMinutes(1), verificationId);

        assertRejected(CORRECT_CODE, "인증번호가 만료되었습니다.");
        assertThat(attemptCount()).isZero();
        assertNotVerified();
    }

    @Test
    @DisplayName("확인한 인증번호를 다시 제출해도 새 인증 증명을 발급하지 않는다")
    void confirmedCode_cannotBeReused() {
        verificationService.confirm(request(CORRECT_CODE));
        EmailVerification before = savedVerification();

        assertRejected(CORRECT_CODE, "이미 확인한 인증번호입니다.");

        EmailVerification after = savedVerification();
        assertThat(after.getVerificationTokenHash())
                .isEqualTo(before.getVerificationTokenHash());
        assertThat(after.getVerifiedDt()).isEqualTo(before.getVerifiedDt());
        assertThat(after.getTokenExpiresDt()).isEqualTo(before.getTokenExpiresDt());
        assertThat(after.getAttemptCount()).isZero();
    }

    private void assertRejected(String code, String reason) {
        assertThatThrownBy(() -> verificationService.confirm(request(code)))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception -> {
                    assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
                    assertThat(exception.getReason()).contains(reason);
                });
    }

    private EmailVerificationConfirmRequestDto request(String code) {
        EmailVerificationConfirmRequestDto request =
                new EmailVerificationConfirmRequestDto();
        // DTO에 setter가 없으므로 테스트에서만 필드를 설정한다.
        ReflectionTestUtils.setField(request, "emailVerificationId", verificationId);
        ReflectionTestUtils.setField(request, "verificationCode", code);
        return request;
    }

    private Integer attemptCount() {
        return jdbcTemplate.queryForObject("""
                SELECT attempt_count
                FROM zipbob_app.email_verification_mng
                WHERE email_verification_id = ?
                """, Integer.class, verificationId);
    }

    private EmailVerification savedVerification() {
        return verificationRepository.findById(verificationId).orElseThrow();
    }

    private void assertNotVerified() {
        EmailVerification saved = savedVerification();
        assertThat(saved.getVerifiedDt()).isNull();
        assertThat(saved.getVerificationTokenHash()).isNull();
        assertThat(saved.getTokenExpiresDt()).isNull();
        assertThat(saved.getUsedDt()).isNull();
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class PasswordConfig {
        @Bean
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }
    }
}
