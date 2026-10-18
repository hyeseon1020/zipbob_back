package com.zipbob.zipbob_backend.service;

import com.zipbob.zipbob_backend.dto.EmailVerificationConfirmRequestDto;
import com.zipbob.zipbob_backend.dto.EmailVerificationConfirmResponseDto;
import com.zipbob.zipbob_backend.dto.EmailVerificationSendRequestDto;
import com.zipbob.zipbob_backend.dto.EmailVerificationSendResponseDto;
import com.zipbob.zipbob_backend.entity.EmailVerification;
import com.zipbob.zipbob_backend.repository.EmailVerificationRepository;
import com.zipbob.zipbob_backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.MailException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.sql.PreparedStatement;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class EmailVerificationService {

    private final EmailVerificationRepository verificationRepository;
    private final UserRepository userRepository;
    private final EmailSendService emailSendService;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    private static final String SIGNUP = "SIGNUP";

    // 인증번호 유효시간: 5분
    private static final long CODE_TTL_SECONDS = 300;

    // 인증 완료 후 발급하는 증명의 유효시간: 10분
    private static final long TOKEN_TTL_SECONDS = 600;

    // 같은 이메일·목적의 재발송 간격: 60초
    private static final long RESEND_INTERVAL_SECONDS = 60;

    // 인증번호 입력 실패 허용 횟수
    private static final int MAX_ATTEMPTS = 5;

    private static final SecureRandom RANDOM = new SecureRandom();

    private static final Pattern EMAIL_LOCAL_PATTERN =
            Pattern.compile(
                    "[a-z0-9!#$%&'*+/=?^_`{|}~-]+"
                            + "(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*"
            );

    private static final Pattern EMAIL_DOMAIN_PATTERN =
            Pattern.compile(
                    "[a-z0-9](?:[a-z0-9-]*[a-z0-9])?"
                            + "(?:\\.[a-z0-9](?:[a-z0-9-]*[a-z0-9])?)+"
            );

    private static final Pattern CODE_PATTERN =
            Pattern.compile("[0-9]{6}");

    // 32바이트 난수를 Base64 URL 형식으로 인코딩한 토큰
    private static final Pattern TOKEN_PATTERN =
            Pattern.compile("[A-Za-z0-9_-]{43}");

    // 1. 인증번호 발송
    @Transactional
    public EmailVerificationSendResponseDto send(
            EmailVerificationSendRequestDto request
    ) {
        if (request == null) {
            throw badRequest("이메일 인증 정보를 입력해 주세요.");
        }

        String email = normalizeEmailPart(
                request.getEmail(),
                "이메일 아이디를 입력해 주세요."
        );

        String emailAdr = normalizeEmailPart(
                request.getEmailAdr(),
                "이메일 도메인을 입력해 주세요."
        );

        validateEmail(email, emailAdr);

        String purpose = requiredText(
                request.getPurpose(),
                "인증 목적을 입력해 주세요."
        );

        // 세 가지 인증 목적만 허용
        if (!java.util.Set.of(
                SIGNUP,
                "FIND_ID",
                "RESET_PASSWORD"
        ).contains(purpose)) {
            throw badRequest("지원하지 않는 인증 목적입니다.");
        }

        // 같은 이메일·목적의 요청을 순서대로 처리
        lockEmail(email, emailAdr, purpose);

        LocalDateTime now = LocalDateTime.now();

        // 회원가입에서만 이미 가입된 이메일을 거절
        if (SIGNUP.equals(purpose)
                && userRepository.existsByEmailAndEmailAdr(email, emailAdr)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 가입된 이메일입니다."
            );
        }

        // 같은 이메일·목적의 최근 발송 시각 확인
        verificationRepository
                .findFirstByEmailAndEmailAdrAndPurposeOrderByCreatedDtDescEmailVerificationIdDesc(
                        email,
                        emailAdr,
                        purpose
                )
                .ifPresent(latest -> {
                    LocalDateTime nextSendTime =
                            latest.getCreatedDt()
                                    .plusSeconds(RESEND_INTERVAL_SECONDS);

                    if (now.isBefore(nextSendTime)) {
                        throw new ResponseStatusException(
                                HttpStatus.TOO_MANY_REQUESTS,
                                "인증번호는 발송 후 60초가 지나면 다시 요청할 수 있습니다."
                        );
                    }
                });

        // 같은 이메일·목적의 기존 인증번호와 인증 증명을 만료
        jdbcTemplate.update("""
                UPDATE zipbob_app.email_verification_mng
                SET expires_dt = ?,
                    token_expires_dt = ?
                WHERE email = ?
                  AND email_adr = ?
                  AND purpose = ?
                  AND used_dt IS NULL
                """,
                now,
                now,
                email,
                emailAdr,
                purpose
        );

        String verificationCode = String.format(
                Locale.ROOT,
                "%06d",
                RANDOM.nextInt(1_000_000)
        );

        LocalDateTime expiresDt =
                now.plusSeconds(CODE_TTL_SECONDS);

        EmailVerification verification = new EmailVerification(
                email,
                emailAdr,
                purpose,
                passwordEncoder.encode(verificationCode),
                expiresDt,
                now
        );

        // 메일 발송 전에 DB 제약조건 오류 확인
        verificationRepository.saveAndFlush(verification);

        try {
            emailSendService.sendVerificationCode(
                    email,
                    emailAdr,
                    verificationCode,
                    CODE_TTL_SECONDS
            );
        } catch (MailException exception) {
            throw new ResponseStatusException(
                    HttpStatus.SERVICE_UNAVAILABLE,
                    "인증 메일 발송에 실패했습니다. 잠시 후 다시 시도해 주세요.",
                    exception
            );
        }

        long remainingSeconds = Math.max(
                0,
                Duration.between(
                        LocalDateTime.now(),
                        expiresDt
                ).getSeconds()
        );

        return new EmailVerificationSendResponseDto(
                verification.getEmailVerificationId(),
                expiresDt,
                remainingSeconds
        );
    }

    // 2. 인증번호 확인
    // 번호 불일치로 예외가 발생해도 실패 횟수는 저장
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public EmailVerificationConfirmResponseDto confirm(
            EmailVerificationConfirmRequestDto request
    ) {
        if (request == null
                || request.getEmailVerificationId() == null
                || request.getEmailVerificationId() <= 0) {
            throw badRequest("올바른 인증 요청 ID가 필요합니다.");
        }

        String verificationCode =
                request.getVerificationCode();

        if (verificationCode == null
                || !CODE_PATTERN.matcher(verificationCode).matches()) {
            throw badRequest("인증번호는 숫자 6자리로 입력해 주세요.");
        }

        // 이메일 잠금에 필요한 정보 조회
        EmailVerification reference = verificationRepository
                .findById(request.getEmailVerificationId())
                .orElseThrow(() ->
                        badRequest("인증 요청을 찾을 수 없습니다.")
                );

        lockEmail(
                reference.getEmail(),
                reference.getEmailAdr(),
                reference.getPurpose()
        );

        // 인증 기록 행 잠금
        EmailVerification verification = verificationRepository
                .findByIdForUpdate(request.getEmailVerificationId())
                .orElseThrow(() ->
                        badRequest("인증 요청을 찾을 수 없습니다.")
                );

        // 잠금 대기 중 변경되었을 수 있으므로 최신 상태 반영
        refreshVerification(verification);

        LocalDateTime now = LocalDateTime.now();

        if (!java.util.Set.of(
                SIGNUP,
                "FIND_ID",
                "RESET_PASSWORD"
        ).contains(verification.getPurpose())) {
            throw badRequest("지원하지 않는 인증 목적입니다.");
        }

        if (verification.getUsedDt() != null) {
            throw badRequest("이미 사용한 인증입니다.");
        }

        if (verification.getVerifiedDt() != null) {
            throw badRequest(
                    "이미 확인한 인증번호입니다. 필요한 경우 재발송해 주세요."
            );
        }

        if (!verification.getExpiresDt().isAfter(now)) {
            throw badRequest(
                    "인증번호가 만료되었습니다. 다시 발송해 주세요."
            );
        }

        if (verification.getAttemptCount() >= MAX_ATTEMPTS) {
            throw badRequest(
                    "인증 시도 횟수를 초과했습니다. 다시 발송해 주세요."
            );
        }

        if (!passwordEncoder.matches(
                verificationCode,
                verification.getVerificationCodeHash()
        )) {
            verification.increaseAttemptCount();

            int remainingAttempts =
                    MAX_ATTEMPTS - verification.getAttemptCount();

            if (remainingAttempts == 0) {
                throw badRequest(
                        "인증 시도 횟수를 초과했습니다. 다시 발송해 주세요."
                );
            }

            throw badRequest(
                    "인증번호가 일치하지 않습니다. 남은 횟수: "
                            + remainingAttempts
            );
        }

        String verificationToken = createToken();

        LocalDateTime tokenExpiresDt =
                now.plusSeconds(TOKEN_TTL_SECONDS);

        verification.markVerified(
                hashToken(verificationToken),
                tokenExpiresDt,
                now
        );

        return new EmailVerificationConfirmResponseDto(
                verificationToken,
                tokenExpiresDt,
                TOKEN_TTL_SECONDS
        );
    }

    // 3. 기존 회원가입에서 사용하는 진입 메서드 유지
    @Transactional(propagation = Propagation.MANDATORY)
    public void verifyAndUseForSignup(
            String email,
            String emailAdr,
            String verificationToken
    ) {
        verifyAndUse(
                email,
                emailAdr,
                verificationToken,
                SIGNUP
        );
    }

    // 4. 목적별 인증 증명 검증 및 사용 처리
    // 반드시 호출한 서비스의 트랜잭션 안에서 실행
    @Transactional(propagation = Propagation.MANDATORY)
    public void verifyAndUse(
            String email,
            String emailAdr,
            String verificationToken,
            String purpose
    ) {
        if (!java.util.Set.of(
                SIGNUP,
                "FIND_ID",
                "RESET_PASSWORD"
        ).contains(purpose)) {
            throw badRequest("지원하지 않는 인증 목적입니다.");
        }

        String normalizedEmail = normalizeEmailPart(
                email,
                "이메일 아이디를 입력해 주세요."
        );

        String normalizedEmailAdr = normalizeEmailPart(
                emailAdr,
                "이메일 도메인을 입력해 주세요."
        );

        validateEmail(
                normalizedEmail,
                normalizedEmailAdr
        );

        if (verificationToken == null
                || !TOKEN_PATTERN.matcher(verificationToken).matches()) {
            throw badRequest("유효한 이메일 인증 증명이 필요합니다.");
        }

        lockEmail(
                normalizedEmail,
                normalizedEmailAdr,
                purpose
        );

        EmailVerification verification = verificationRepository
                .findByTokenHashForUpdate(
                        hashToken(verificationToken)
                )
                .orElseThrow(() ->
                        badRequest("이메일 인증 증명이 유효하지 않습니다.")
                );

        refreshVerification(verification);

        LocalDateTime now = LocalDateTime.now();

        // 가입용 인증을 비밀번호 재설정 등에 사용할 수 없도록 검사
        if (!purpose.equals(verification.getPurpose())) {
            throw badRequest(
                    "요청한 작업에 맞는 이메일 인증이 필요합니다."
            );
        }

        // 인증한 이메일과 실제 작업 대상 이메일 일치 확인
        if (!normalizedEmail.equals(verification.getEmail())
                || !normalizedEmailAdr.equals(verification.getEmailAdr())) {
            throw badRequest(
                    "인증한 이메일과 요청한 이메일이 다릅니다."
            );
        }

        if (verification.getVerifiedDt() == null) {
            throw badRequest(
                    "이메일 인증을 먼저 완료해 주세요."
            );
        }

        if (verification.getUsedDt() != null) {
            throw badRequest(
                    "이미 사용한 이메일 인증 증명입니다."
            );
        }

        if (verification.getTokenExpiresDt() == null
                || !verification.getTokenExpiresDt().isAfter(now)) {
            throw badRequest(
                    "이메일 인증 증명이 만료되었습니다. 다시 인증해 주세요."
            );
        }

        verification.markUsed(now);
    }

    /*
     PostgreSQL 트랜잭션 단위 advisory lock

     같은 이메일·목적의 발송, 확인, 사용 요청을 순서대로 처리
     트랜잭션 종료 시 잠금 해제

     JdbcTemplate과 JPA는 동일한 DataSource를 사용
     */
    private void lockEmail(
            String email,
            String emailAdr,
            String purpose
    ) {
        String lockKey =
                "email-verification:"
                        + purpose
                        + ":"
                        + email
                        + "@"
                        + emailAdr;

        jdbcTemplate.execute((ConnectionCallback<Void>) connection -> {
            try (PreparedStatement statement = connection.prepareStatement(
                    "SELECT pg_advisory_xact_lock(hashtextextended(?, 0))"
            )) {
                statement.setString(1, lockKey);
                statement.execute();
            }

            return null;
        });
    }

    private static String normalizeEmailPart(
            String value,
            String message
    ) {
        return requiredText(value, message)
                .toLowerCase(Locale.ROOT);
    }

    private static void validateEmail(
            String email,
            String emailAdr
    ) {
        if (email.length() > 50
                || !EMAIL_LOCAL_PATTERN.matcher(email).matches()) {
            throw badRequest("이메일 아이디 형식을 확인해 주세요.");
        }

        if (emailAdr.length() > 50
                || !EMAIL_DOMAIN_PATTERN.matcher(emailAdr).matches()) {
            throw badRequest("이메일 도메인 형식을 확인해 주세요.");
        }
    }

    private static String requiredText(
            String value,
            String message
    ) {
        if (value == null || value.isBlank()) {
            throw badRequest(message);
        }

        return value.trim();
    }

    // 인증 증명 원문 생성
    private static String createToken() {
        byte[] bytes = new byte[32];

        RANDOM.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

    // 인증 증명은 SHA-256 해시로 DB에 저장
    private static String hashToken(String token) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256")
                    .digest(token.getBytes(StandardCharsets.UTF_8));

            return HexFormat.of().formatHex(hash);

        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(
                    "인증 증명 해시 생성에 실패했습니다.",
                    exception
            );
        }
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                message
        );
    }

    @jakarta.persistence.PersistenceContext
    private jakarta.persistence.EntityManager entityManager;

    private void refreshVerification(
            EmailVerification verification
    ) {
        entityManager.refresh(verification);
    }
}