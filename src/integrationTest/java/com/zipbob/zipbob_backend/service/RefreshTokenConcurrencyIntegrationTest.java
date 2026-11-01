package com.zipbob.zipbob_backend.service;

import com.zipbob.zipbob_backend.dto.RefreshTokenRequestDto;
import com.zipbob.zipbob_backend.dto.TokenRefreshResponseDto;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.RepeatedTest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import com.zipbob.zipbob_backend.security.JwtTokenProvider;

import java.security.SecureRandom;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("integration")
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import({RefreshTokenService.class, JwtTokenProvider.class})
@Transactional(propagation = Propagation.NOT_SUPPORTED)
// Refresh Token 동시 요청 테스트
class RefreshTokenConcurrencyIntegrationTest {

    @Autowired private RefreshTokenService refreshTokenService;
    @Autowired private JwtTokenProvider jwtTokenProvider;
    @Autowired private JdbcTemplate jdbcTemplate;
    @Autowired private PlatformTransactionManager transactionManager;

    private String userId;
    private String rawToken;
    private String tokenHash;
    private String workerLabel;
    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        String suffix = UUID.randomUUID().toString().replace("-", "");
        userId = "it_refresh_" + suffix;
        workerLabel = "refresh-race-" + suffix;
        byte[] bytes = new byte[32];
        new SecureRandom().nextBytes(bytes);
        rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tokenHash = refreshTokenService.hashToken(rawToken);

        // 요청 스레드들이 조회할 수 있도록 준비 데이터를 먼저 커밋한다.
        jdbcTemplate.update("""
                INSERT INTO zipbob_app.user_mng
                    (user_id, user_pw, user_name, user_role, created_id)
                VALUES (?, 'unused-test-password', ?, 'USER', ?)
                """, userId, userId, userId);
        jdbcTemplate.update("""
                INSERT INTO zipbob_app.user_refresh_token_mng
                    (user_id, token_hash, created_dt, expires_dt)
                VALUES (?, ?, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP + INTERVAL '14 days')
                """, userId, tokenHash);
        executor = Executors.newFixedThreadPool(2);
    }

    @AfterEach
    void tearDown() throws InterruptedException {
        // 실행 중인 작업이 DB 정리 이후 데이터를 다시 쓰지 않도록 종료를 기다린다.
        if (executor != null) {
            executor.shutdownNow();
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                throw new IllegalStateException("재발급 작업이 종료되지 않아 DB 정리를 중단했습니다.");
            }
        }
        jdbcTemplate.update("DELETE FROM zipbob_app.user_refresh_token_mng WHERE user_id = ?", userId);
        jdbcTemplate.update("DELETE FROM zipbob_app.user_mng WHERE user_id = ?", userId);
    }

    @RepeatedTest(3)
    @DisplayName("동일 토큰의 두 재발급 요청이 겹쳐도 하나만 성공하고 나머지는 401로 거절된다")
    void concurrentRefresh_onlyOneRequestSucceeds() throws Exception {
        TransactionTemplate holder = new TransactionTemplate(transactionManager);
        holder.setTimeout(20);

        // 먼저 행 잠금을 잡고, 두 요청이 실제 PostgreSQL 잠금을 기다리게 한다.
        // 단순히 스레드를 동시에 시작하는 것보다 요청의 겹침을 명확히 검증한다.
        List<Future<Outcome>> futures = holder.execute(status -> {
            jdbcTemplate.queryForObject("""
                    SELECT refresh_token_id
                    FROM zipbob_app.user_refresh_token_mng
                    WHERE token_hash = ?
                    FOR UPDATE
                    """, Long.class, tokenHash);

            List<Future<Outcome>> submitted = List.of(
                    executor.submit(this::refreshInOwnTransaction),
                    executor.submit(this::refreshInOwnTransaction));
            waitForBothDatabaseWaiters();
            return submitted;
        });
        // 위 트랜잭션이 커밋되며 잠금 해제. 두 요청이 같은 토큰을 놓고 경쟁한다.
        assertThat(futures).isNotNull();
        List<Outcome> outcomes = List.of(
                futures.get(0).get(30, TimeUnit.SECONDS),
                futures.get(1).get(30, TimeUnit.SECONDS));
        assertThat(outcomes).filteredOn(outcome -> outcome.response() != null).hasSize(1);
        assertThat(outcomes).filteredOn(outcome -> outcome.status() == 401).hasSize(1);

        TokenRefreshResponseDto winner = outcomes.stream()
                .map(Outcome::response).filter(java.util.Objects::nonNull)
                .findFirst().orElseThrow();
        assertThat(winner.getRefreshToken()).isNotEqualTo(rawToken);
        assertThat(jwtTokenProvider.validateAndGetUserId(winner.getAccessToken())).isEqualTo(userId);
        assertOnlyStoredToken(winner.getRefreshToken());

        // 기존 토큰은 재사용할 수 없고, 성공한 요청의 새 토큰은 정상 사용 가능하다.
        assertThatThrownBy(() -> refreshTokenService.refresh(request(rawToken)))
                .isInstanceOfSatisfying(ResponseStatusException.class, exception ->
                        assertThat(exception.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED));
        TokenRefreshResponseDto next = refreshTokenService.refresh(request(winner.getRefreshToken()));
        assertThat(next.getRefreshToken()).isNotEqualTo(winner.getRefreshToken());
        assertOnlyStoredToken(next.getRefreshToken());
    }

    private Outcome refreshInOwnTransaction() {
        TransactionTemplate transaction = new TransactionTemplate(transactionManager);
        transaction.setTimeout(20);
        try {
            TokenRefreshResponseDto response = transaction.execute(status -> {
                // 이 테스트의 연결만 식별하고, 실패 시에도 기다림을 제한한다.
                jdbcTemplate.queryForObject("SELECT set_config('application_name', ?, true)",
                        String.class, workerLabel);
                jdbcTemplate.execute("SET LOCAL lock_timeout = '15s'");
                jdbcTemplate.execute("SET LOCAL statement_timeout = '20s'");
                return refreshTokenService.refresh(request(rawToken));
            });
            return new Outcome(response, 200);
        } catch (ResponseStatusException exception) {
            return new Outcome(null, exception.getStatusCode().value());
        }
        // 예상하지 않은 DB 오류는 Future.get()에서 테스트 실패로 전달된다.
    }

    private void waitForBothDatabaseWaiters() {
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (System.nanoTime() < deadline) {
            // 같은 트랜잭션에서 통계 조회 결과가 캐시되지 않게 갱신한다.
            jdbcTemplate.execute("SELECT pg_stat_clear_snapshot()");
            Integer waiting = jdbcTemplate.queryForObject("""
                    SELECT COUNT(*) FROM pg_stat_activity
                    WHERE datname = current_database()
                      AND application_name = ?
                      AND wait_event_type = 'Lock'
                    """, Integer.class, workerLabel);
            if (waiting != null && waiting == 2) {
                return;
            }
            try {
                Thread.sleep(25);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException("잠금 대기 확인 중 인터럽트가 발생했습니다.", exception);
            }
        }
        throw new AssertionError("두 재발급 요청의 DB 잠금 대기를 10초 내 확인하지 못했습니다. 연결 풀은 최소 3개가 필요합니다.");
    }

    private void assertOnlyStoredToken(String expectedRawToken) {
        List<String> hashes = jdbcTemplate.queryForList("""
                SELECT token_hash FROM zipbob_app.user_refresh_token_mng WHERE user_id = ?
                """, String.class, userId);
        assertThat(hashes).containsExactly(refreshTokenService.hashToken(expectedRawToken));
    }

    private RefreshTokenRequestDto request(String token) {
        RefreshTokenRequestDto request = new RefreshTokenRequestDto();
        ReflectionTestUtils.setField(request, "refreshToken", token);
        return request;
    }

    private record Outcome(TokenRefreshResponseDto response, int status) {}
}
