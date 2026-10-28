package com.zipbob.zipbob_backend.security;

import com.zipbob.zipbob_backend.admin.service.LoginService;
import com.zipbob.zipbob_backend.config.AdminSecurityConfig;
import com.zipbob.zipbob_backend.config.SecurityConfig;
import com.zipbob.zipbob_backend.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest
@ContextConfiguration(classes = AdminSecurityTest.TestController.class)
@Import({
        AdminSecurityConfig.class,
        SecurityConfig.class,
        AdminSecurityTest.PasswordConfig.class
})
// 관리자 권한 테스트
class AdminSecurityTest {

    private static final String ADMIN_API = "/api/admin/security-test";

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LoginService loginService;

    @MockitoBean
    private JwtTokenProvider jwtTokenProvider;

    @MockitoBean
    private UserRepository userRepository;

    @Test
    @DisplayName("비로그인 사용자의 관리자 화면 접근은 로그인 페이지로 이동한다")
    void adminPage_redirectsAnonymousToLogin() throws Exception {
        mockMvc.perform(get("/admin/dashboard")
                        .servletPath("/admin/dashboard"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/admin/login"));
    }

    @Test
    @DisplayName("비로그인 사용자의 관리자 API 접근은 JSON 401을 반환한다")
    void adminApi_rejectsAnonymous() throws Exception {
        mockMvc.perform(get(ADMIN_API)
                        .servletPath(ADMIN_API))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message")
                        .value("관리자 로그인이 필요합니다."));
    }

    @Test
    @DisplayName("일반 회원 세션으로 관리자 API에 접근하면 403을 반환한다")
    void adminApi_rejectsUserRole() throws Exception {
        mockMvc.perform(get(ADMIN_API)
                        .servletPath(ADMIN_API)
                        .session(sessionWithRole("USER")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("관리자 세션으로 관리자 API에 접근하면 성공한다")
    void adminApi_allowsAdminRole() throws Exception {
        mockMvc.perform(get(ADMIN_API)
                        .servletPath(ADMIN_API)
                        .session(sessionWithRole("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(content().string("admin-ok"));
    }

    @Test
    @DisplayName("관리자라도 CSRF 토큰 없는 변경 요청은 거부한다")
    void adminMutation_rejectsMissingCsrf() throws Exception {
        mockMvc.perform(post(ADMIN_API)
                        .servletPath(ADMIN_API)
                        .session(sessionWithRole("ADMIN")))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403));
    }

    @Test
    @DisplayName("관리자 세션과 CSRF 토큰이 있는 변경 요청은 성공한다")
    void adminMutation_allowsValidCsrf() throws Exception {
        mockMvc.perform(post(ADMIN_API)
                        .servletPath(ADMIN_API)
                        .session(sessionWithRole("ADMIN"))
                        .with(csrf()))
                .andExpect(status().isOk())
                .andExpect(content().string("updated"));
    }

    @Test
    @DisplayName("관리자 세션만으로 JWT가 필요한 앱 API에 접근할 수 없다")
    void appApi_doesNotUseAdminSession() throws Exception {
        mockMvc.perform(get("/api/users/me")
                        .servletPath("/api/users/me")
                        .session(sessionWithRole("ADMIN")))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    private MockHttpSession sessionWithRole(String role) {
        var authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        "security-test-user",
                        null,
                        AuthorityUtils.createAuthorityList("ROLE_" + role)
                );

        SecurityContext context =
                SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);

        MockHttpSession session = new MockHttpSession();
        session.setAttribute(
                HttpSessionSecurityContextRepository
                        .SPRING_SECURITY_CONTEXT_KEY,
                context
        );

        return session;
    }

    @TestConfiguration(proxyBeanMethods = false)
    static class PasswordConfig {

        @Bean
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder();
        }
    }

    @RestController
    static class TestController {

        @GetMapping("/admin/dashboard")
        String dashboard() {
            return "dashboard";
        }

        @GetMapping("/api/admin/security-test")
        String adminApi() {
            return "admin-ok";
        }

        @PostMapping("/api/admin/security-test")
        String update() {
            return "updated";
        }

        @GetMapping("/api/users/me")
        String me() {
            return "app-user";
        }
    }
}