package com.zipbob.zipbob_backend.security;

import com.zipbob.zipbob_backend.config.SecurityConfig;
import com.zipbob.zipbob_backend.repository.UserRepository;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
@ContextConfiguration(classes = ApiSecurityTest.TestController.class)
@Import(SecurityConfig.class)
// 앱 API 인증 테스트
class ApiSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    @DisplayName("공개 메뉴 조회는 토큰 없이 접근할 수 있다")
    void publicEndpoint_allowsAnonymous() throws Exception {
        mockMvc.perform(get("/api/menus"))
                .andExpect(status().isOk())
                .andExpect(content().string("public"));

        verifyNoInteractions(jwtTokenProvider, userRepository);
    }

    @Test
    @DisplayName("회원 전용 API는 토큰이 없으면 401을 반환한다")
    void protectedEndpoint_rejectsMissingToken() throws Exception {
        mockMvc.perform(get("/api/users/me"))
                .andExpect(status().isUnauthorized())
                .andExpect(header().string(
                        HttpHeaders.WWW_AUTHENTICATE, "Bearer"
                ))
                .andExpect(jsonPath("$.status").value(401));

        verifyNoInteractions(jwtTokenProvider, userRepository);
    }

    @Test
    @DisplayName("잘못된 Authorization 형식은 401을 반환한다")
    void protectedEndpoint_rejectsInvalidHeader() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .header(HttpHeaders.AUTHORIZATION, "Basic invalid"))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(jwtTokenProvider, userRepository);
    }

    @Test
    @DisplayName("유효하지 않은 JWT는 401을 반환한다")
    void protectedEndpoint_rejectsInvalidToken() throws Exception {
        when(jwtTokenProvider.validateAndGetUserId("invalid-token"))
                .thenThrow(new JwtException("Invalid token"));

        mockMvc.perform(get("/api/users/me")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer invalid-token"
                        ))
                .andExpect(status().isUnauthorized());

        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("삭제된 회원의 JWT는 401을 반환한다")
    void protectedEndpoint_rejectsDeletedUser() throws Exception {
        when(jwtTokenProvider.validateAndGetUserId("valid-token"))
                .thenReturn("deleted-user");
        when(userRepository.existsById("deleted-user"))
                .thenReturn(false);

        mockMvc.perform(get("/api/users/me")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer valid-token"
                        ))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("유효한 JWT와 존재하는 회원은 회원 전용 API에 접근한다")
    void protectedEndpoint_allowsAuthenticatedUser() throws Exception {
        when(jwtTokenProvider.validateAndGetUserId("valid-token"))
                .thenReturn("test-user");
        when(userRepository.existsById("test-user"))
                .thenReturn(true);

        mockMvc.perform(get("/api/users/me")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer valid-token"
                        ))
                .andExpect(status().isOk())
                .andExpect(content().string("test-user"));
    }

    @Test
    @DisplayName("인증된 회원도 허용하지 않은 경로에는 접근할 수 없다")
    void unmatchedEndpoint_rejectsAuthenticatedUser() throws Exception {
        when(jwtTokenProvider.validateAndGetUserId("valid-token"))
                .thenReturn("test-user");
        when(userRepository.existsById("test-user"))
                .thenReturn(true);

        mockMvc.perform(get("/api/security-test-denied")
                        .header(
                                HttpHeaders.AUTHORIZATION,
                                "Bearer valid-token"
                        ))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    // 보안 필터 통과 여부와 인증된 사용자만 확인하는 테스트용 컨트롤러
    @RestController
    static class TestController {

        @GetMapping("/api/menus")
        String menus() {
            return "public";
        }

        @GetMapping("/api/users/me")
        String me(Principal principal) {
            return principal.getName();
        }

        @GetMapping("/api/security-test-denied")
        String denied() {
            return "must not reach";
        }
    }
}