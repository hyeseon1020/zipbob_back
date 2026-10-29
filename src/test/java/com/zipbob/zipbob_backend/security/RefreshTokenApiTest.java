package com.zipbob.zipbob_backend.security;

import com.jayway.jsonpath.JsonPath;
import com.zipbob.zipbob_backend.entity.User;
import com.zipbob.zipbob_backend.repository.UserRefreshTokenRepository;
import com.zipbob.zipbob_backend.repository.UserRepository;
import com.zipbob.zipbob_backend.service.RefreshTokenService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
// 토큰 교체 → 이전 토큰 401 → 새 토큰으로 로그아웃 → 재사용 401을 HTTP 검증
class RefreshTokenApiTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private EntityManager entityManager;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private RefreshTokenService refreshTokenService;

    @Autowired
    private UserRefreshTokenRepository refreshTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtTokenProvider jwtTokenProvider;

    private String userId;
    private String originalToken;

    @BeforeEach
    void setUp() {
        userId = "rt-" + UUID.randomUUID();

        // 테스트 회원과 최초 토큰을 저장하고 커밋한다.
        originalToken = transactionTemplate.execute(status -> {
            User user = BeanUtils.instantiateClass(User.class);

            ReflectionTestUtils.setField(user, "userId", userId);
            ReflectionTestUtils.setField(
                    user, "userPw", "test-password-hash"
            );
            ReflectionTestUtils.setField(
                    user, "userName", "토큰API테스트회원"
            );

            entityManager.persist(user);

            return refreshTokenService.issue(user);
        });
    }

    @AfterEach
    void tearDown() {
        // 이 테스트에서 생성한 회원과 토큰만 정리한다.
        transactionTemplate.executeWithoutResult(status -> {
            refreshTokenRepository.deleteAllByUserId(userId);
            userRepository.deleteById(userId);
        });
    }

    @Test
    @DisplayName("재발급 API는 유효한 JWT와 새 Refresh Token을 반환한다")
    void refresh_returnsValidTokens() throws Exception {
        String responseBody = mockMvc.perform(
                        post("/api/users/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(tokenBody(originalToken))
                )
                .andExpect(status().isOk())
                .andExpect(header().string(
                        "Cache-Control", "no-store"
                ))
                .andExpect(header().string(
                        "Pragma", "no-cache"
                ))
                .andExpect(jsonPath(
                        "$.accessToken",
                        not(isEmptyOrNullString())
                ))
                .andExpect(jsonPath(
                        "$.refreshToken",
                        not(isEmptyOrNullString())
                ))
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.expiresIn").value(1800))
                .andReturn()
                .getResponse()
                .getContentAsString();

        String accessToken =
                JsonPath.read(responseBody, "$.accessToken");
        String newRefreshToken =
                JsonPath.read(responseBody, "$.refreshToken");

        assertThat(jwtTokenProvider.validateAndGetUserId(accessToken))
                .isEqualTo(userId);
        assertThat(newRefreshToken).isNotEqualTo(originalToken);

        // 새 토큰으로 다음 요청에서도 정상 재발급된다.
        mockMvc.perform(
                        post("/api/users/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(tokenBody(newRefreshToken))
                )
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("재발급 API에서 교체된 이전 토큰을 재사용하면 401이다")
    void refresh_rejectsOldToken() throws Exception {
        mockMvc.perform(
                        post("/api/users/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(tokenBody(originalToken))
                )
                .andExpect(status().isOk());

        mockMvc.perform(
                        post("/api/users/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(tokenBody(originalToken))
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(header().string(
                        "Cache-Control", "no-store"
                ));
    }

    @Test
    @DisplayName("교체된 새 토큰으로 로그아웃하면 해당 토큰도 재사용할 수 없다")
    void logout_rejectsRefreshAfterRotation() throws Exception {
        String responseBody = mockMvc.perform(
                        post("/api/users/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(tokenBody(originalToken))
                )
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String newRefreshToken =
                JsonPath.read(responseBody, "$.refreshToken");

        mockMvc.perform(
                        post("/api/users/logout")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(tokenBody(newRefreshToken))
                )
                .andExpect(status().isNoContent())
                .andExpect(content().string(""))
                .andExpect(header().string(
                        "Cache-Control", "no-store"
                ));

        mockMvc.perform(
                        post("/api/users/refresh")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(tokenBody(newRefreshToken))
                )
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));

        // 이미 로그아웃한 토큰으로 다시 로그아웃해도 204다.
        mockMvc.perform(
                        post("/api/users/logout")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(tokenBody(newRefreshToken))
                )
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("Refresh Token이 누락되거나 형식이 잘못되면 400이다")
    void endpoints_rejectInvalidInput() throws Exception {
        for (String endpoint : new String[]{
                "/api/users/refresh",
                "/api/users/logout"
        }) {
            for (String body : new String[]{
                    "{}",
                    "{\"refreshToken\":null}",
                    "{\"refreshToken\":\"\"}",
                    "{\"refreshToken\":\"invalid-token\"}"
            }) {
                mockMvc.perform(
                                post(endpoint)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(body)
                        )
                        .andExpect(status().isBadRequest())
                        .andExpect(jsonPath("$.status").value(400));
            }
        }
    }

    private String tokenBody(String token) {
        // 서버가 발급한 Base64 URL 토큰만 전달한다.
        return "{\"refreshToken\":\"" + token + "\"}";
    }
}