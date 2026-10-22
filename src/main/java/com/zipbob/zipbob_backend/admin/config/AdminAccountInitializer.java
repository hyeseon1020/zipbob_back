package com.zipbob.zipbob_backend.admin.config;

import com.zipbob.zipbob_backend.entity.User;
import com.zipbob.zipbob_backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
        prefix = "admin.bootstrap",
        name = "enabled",
        havingValue = "true",
        matchIfMissing = false
)
public class AdminAccountInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final PlatformTransactionManager transactionManager;

    @Value("${admin.bootstrap.id:}")
    private String adminId;

    @Value("${admin.bootstrap.password:}")
    private String adminPassword;

    @Value("${admin.bootstrap.name:}")
    private String adminName;

    @Override
    public void run(ApplicationArguments args) {

        String userId = adminId.trim();

        if (!userId.matches("[A-Za-z0-9_]{4,50}")) {
            throw new IllegalStateException(
                    "ADMIN_BOOTSTRAP_ID는 영문, 숫자, 밑줄을 사용해 "
                            + "4~50자로 설정해 주세요."
            );
        }

        TransactionTemplate transaction =
                new TransactionTemplate(transactionManager);

        Boolean created = transaction.execute(status ->
                createAdminIfMissing(userId)
        );

        // 트랜잭션 커밋이 완료된 뒤 결과만 출력
        // 비밀번호와 해시는 출력하지 않음
        if (Boolean.TRUE.equals(created)) {
            log.info(
                    "관리자 계정 생성 완료. "
                            + "관리자 생성 설정을 끄고 초기 비밀번호 환경변수를 제거해 주세요."
            );
        } else {
            log.info(
                    "기존 관리자 계정이 있어 생성을 건너뜁니다. "
                            + "비밀번호와 권한은 변경하지 않았습니다."
            );
        }
    }

    private boolean createAdminIfMissing(String userId) {

        Optional<User> existingUser =
                userRepository.findById(userId);

        if (existingUser.isPresent()) {
            if (!existingUser.get().isAdmin()) {
                throw new IllegalStateException(
                        "관리자 생성 아이디가 기존 일반 회원과 중복됩니다. "
                                + "다른 관리자 아이디를 설정해 주세요."
                );
            }

            return false;
        }

        String userName = adminName.trim();

        if (!userName.matches("[가-힣A-Za-z0-9_]{2,20}")) {
            throw new IllegalStateException(
                    "ADMIN_BOOTSTRAP_NAME은 한글, 영문, 숫자, 밑줄을 사용해 "
                            + "2~20자로 설정해 주세요."
            );
        }

        validatePassword(adminPassword);

        if (userRepository.existsByUserName(userName)) {
            throw new IllegalStateException(
                    "관리자 닉네임이 기존 회원과 중복됩니다. "
                            + "다른 닉네임을 설정해 주세요."
            );
        }

        // 기존 PasswordConfig의 BCryptPasswordEncoder 사용
        String encodedPassword =
                passwordEncoder.encode(adminPassword);

        int inserted = userRepository.insertAdmin(
                userId,
                encodedPassword,
                userName
        );

        if (inserted != 1) {
            throw new IllegalStateException(
                    "관리자 계정을 생성하지 못했습니다. "
                            + "아이디 또는 닉네임 중복 여부를 확인해 주세요."
            );
        }

        return true;
    }

    private void validatePassword(String password) {

        if (password == null || password.isBlank()) {
            throw new IllegalStateException(
                    "ADMIN_BOOTSTRAP_PASSWORD를 설정해 주세요."
            );
        }

        if (password.length() < 8
                || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException(
                    "관리자 비밀번호는 8자 이상이며 "
                            + "UTF-8 기준 72바이트 이하여야 합니다."
            );
        }

        if (!password.matches(".*[A-Za-z].*")
                || !password.matches(".*[0-9].*")) {
            throw new IllegalStateException(
                    "관리자 비밀번호에는 영문과 숫자를 포함해 주세요."
            );
        }
    }
}