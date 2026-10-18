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
// 인증번호 발송·확인과 회원가입 시 인증 증명 검증
public class EmailVerificationService {

    private final EmailVerificationRepository verificationRepository;
    private final UserRepository userRepository;
    private final EmailSendService emailSendService;
    private final PasswordEncoder passwordEncoder;
    private final JdbcTemplate jdbcTemplate;

    private static final String SIGNUP = "SIGNUP";

    private static final long CODE_TTL_SECONDS = 300;
    private static final long TOKEN_TTL_SECONDS = 600;
    private static final long RESEND_INTERVAL_SECONDS = 60;

    private static final int MAX_ATTEMPTS = 5;

    private static final SecureRandom RANDOM = new SecureRandom();

    // 기존 회원가입과 동일한 이메일 형식
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

    // 32바이트 난수를 Base64 URL 형식으로 인코딩하면 43자리
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

        // 아이디 찾기·비밀번호 재설정은 해당 기능 구현 후 허용
        if (!SIGNUP.equals(purpose)) {
            throw badRequest("현재 회원가입용 이메일 인증만 지원합니다.");
        }

        // 같은 이메일·목적의 요청을 순서대로 처리
        // 최초 발송처럼 잠글 인증 행이 없는 경우에도 적용
        lockEmail(email, emailAdr, purpose);

        LocalDateTime now = LocalDateTime.now();

        if (userRepository.existsByEmailAndEmailAdr(email, emailAdr)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 가입된 이메일입니다."
            );
        }

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

        // 재발송하면 기존 인증번호와 인증 증명을 모두 만료
        // 발송에 실패하면 같은 트랜잭션에서 이 변경도 취소됨
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

        LocalDateTime expiresDt = now.plusSeconds(CODE_TTL_SECONDS);

        EmailVerification verification = new EmailVerification(
                email,
                emailAdr,
                purpose,
                passwordEncoder.encode(verificationCode),
                expiresDt,
                now
        );

        // DB 제약조건 오류를 메일 발송 전에 확인
        verificationRepository.saveAndFlush(verification);

        try {
            emailSendService.sendVerificationCode(
                    email,
                    emailAdr,
                    verificationCode,
                    CODE_TTL_SECONDS
            );
        } catch (MailException exception) {
            // 기본 트랜잭션 동작에 따라 인증 기록 저장도 취소
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
    // 번호 불일치로 예외를 던져도 실패 횟수는 DB에 반영해야 함
    @Transactional(noRollbackFor = ResponseStatusException.class)
    public EmailVerificationConfirmResponseDto confirm(
            EmailVerificationConfirmRequestDto request
    ) {
        if (request == null
                || request.getEmailVerificationId() == null
                || request.getEmailVerificationId() <= 0) {
            throw badRequest("올바른 인증 요청 ID가 필요합니다.");
        }

        String verificationCode = request.getVerificationCode();

        if (verificationCode == null
                || !CODE_PATTERN.matcher(verificationCode).matches()) {
            throw badRequest("인증번호는 숫자 6자리로 입력해 주세요.");
        }

        // 이메일 잠금에 필요한 정보를 먼저 조회
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

        EmailVerification verification = verificationRepository
                .findByIdForUpdate(request.getEmailVerificationId())
                .orElseThrow(() ->
                        badRequest("인증 요청을 찾을 수 없습니다.")
                );

        /*
         첫 조회 이후 잠금을 기다리는 동안 다른 요청이
         기록을 수정했을 수 있으므로 DB의 최신 상태로 갱신.
         */
        refreshVerification(verification);

        LocalDateTime now = LocalDateTime.now();

        if (!SIGNUP.equals(verification.getPurpose())) {
            throw badRequest("지원하지 않는 인증 목적입니다.");
        }

        if (verification.getUsedDt() != null) {
            throw badRequest("이미 사용한 인증입니다.");
        }

        if (verification.getVerifiedDt() != null) {
            throw badRequest("이미 확인한 인증번호입니다. 필요한 경우 재발송해 주세요.");
        }

        if (!verification.getExpiresDt().isAfter(now)) {
            throw badRequest("인증번호가 만료되었습니다. 다시 발송해 주세요.");
        }

        if (verification.getAttemptCount() >= MAX_ATTEMPTS) {
            throw badRequest("인증 시도 횟수를 초과했습니다. 다시 발송해 주세요.");
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

    /*
     3. 회원가입용 인증 증명 검증 및 사용 처리
     반드시 회원가입 트랜잭션 안에서 호출
     회원 저장에 실패하면 인증 증명 사용 처리도 함께 취소
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public void verifyAndUseForSignup(
            String email,
            String emailAdr,
            String verificationToken
    ) {
        String normalizedEmail = normalizeEmailPart(
                email,
                "이메일 아이디를 입력해 주세요."
        );

        String normalizedEmailAdr = normalizeEmailPart(
                emailAdr,
                "이메일 도메인을 입력해 주세요."
        );

        validateEmail(normalizedEmail, normalizedEmailAdr);

        if (verificationToken == null
                || !TOKEN_PATTERN.matcher(verificationToken).matches()) {
            throw badRequest("유효한 이메일 인증 증명이 필요합니다.");
        }

        lockEmail(normalizedEmail, normalizedEmailAdr, SIGNUP);

        EmailVerification verification = verificationRepository
                .findByTokenHashForUpdate(hashToken(verificationToken))
                .orElseThrow(() ->
                        badRequest("이메일 인증 증명이 유효하지 않습니다.")
                );

        refreshVerification(verification);

        LocalDateTime now = LocalDateTime.now();

        if (!SIGNUP.equals(verification.getPurpose())) {
            throw badRequest("회원가입용 이메일 인증이 필요합니다.");
        }

        if (!normalizedEmail.equals(verification.getEmail())
                || !normalizedEmailAdr.equals(verification.getEmailAdr())) {
            throw badRequest("인증한 이메일과 가입할 이메일이 다릅니다.");
        }

        if (verification.getVerifiedDt() == null) {
            throw badRequest("이메일 인증을 먼저 완료해 주세요.");
        }

        if (verification.getUsedDt() != null) {
            throw badRequest("이미 사용한 이메일 인증 증명입니다.");
        }

        if (verification.getTokenExpiresDt() == null
                || !verification.getTokenExpiresDt().isAfter(now)) {
            throw badRequest("이메일 인증 증명이 만료되었습니다. 다시 인증해 주세요.");
        }

        verification.markUsed(now);
    }

    /*
     공통 처리

     PostgreSQL 트랜잭션 단위 advisory lock.
     같은 이메일·목적의 발송, 확인, 사용 요청을 순서대로 처리.
     트랜잭션 종료 시 DB가 잠금을 해제.

     JdbcTemplate은 JPA와 동일한 DataSource를 사용해야 함.
     */
    private void lockEmail(
            String email,
            String emailAdr,
            String purpose
    ) {
        String lockKey =
                "email-verification:" + purpose + ":" + email + "@" + emailAdr;

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
        return requiredText(value, message).toLowerCase(Locale.ROOT);
    }

    private static void validateEmail(String email, String emailAdr) {
        if (email.length() > 50
                || !EMAIL_LOCAL_PATTERN.matcher(email).matches()) {
            throw badRequest("이메일 아이디 형식을 확인해 주세요.");
        }

        if (emailAdr.length() > 50
                || !EMAIL_DOMAIN_PATTERN.matcher(emailAdr).matches()) {
            throw badRequest("이메일 도메인 형식을 확인해 주세요.");
        }
    }

    private static String requiredText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw badRequest(message);
        }

        return value.trim();
    }

    private static String createToken() {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(bytes);
    }

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

    private void refreshVerification(EmailVerification verification) {
        entityManager.refresh(verification);
    }
}