package com.zipbob.zipbob_backend.config;

import com.zipbob.zipbob_backend.admin.service.LoginService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint;

import java.io.IOException;
// 관리자 보안 설정 신규 생성
@Configuration
public class AdminSecurityConfig {

    @Bean
    // 관리자 보안 설정을 먼저 확인
    @Order(1)
    public SecurityFilterChain adminSecurityFilterChain(
            HttpSecurity http,
            LoginService loginService,
            PasswordEncoder passwordEncoder
    ) throws Exception {

        DaoAuthenticationProvider provider =
                new DaoAuthenticationProvider(loginService);
        provider.setPasswordEncoder(passwordEncoder);

        LoginUrlAuthenticationEntryPoint loginEntryPoint =
                new LoginUrlAuthenticationEntryPoint("/admin/login");

        http
                // 관리자 화면·API에 이 설정 적용
                .securityMatcher(
                        "/admin", "/admin/**",
                        "/api/admin", "/api/admin/**"
                )

                // 관리자 계정 조회 + BCrypt 비밀번호 검증
                .authenticationManager(new ProviderManager(provider))

                // 세션 인증을 사용하는 관리자 요청은 CSRF 보호
                .csrf(Customizer.withDefaults())

                .sessionManagement(session -> session
                        // 로그인 등에 필요한 경우 세션 생성
                        .sessionCreationPolicy(SessionCreationPolicy.IF_REQUIRED)
                        // 로그인 성공 시 세션 ID 교체
                        .sessionFixation(fixation -> fixation.changeSessionId())
                )

                // 로그인 성공 후 항상 대시보드로 이동
                .requestCache(cache -> cache.disable())
                .httpBasic(basic -> basic.disable())

                .authorizeHttpRequests(auth -> auth
                        // 로그인 화면과 정적 리소스는 공개
                        .requestMatchers(
                                HttpMethod.GET,
                                "/admin/login",
                                "/admin/css/**",
                                "/admin/js/**",
                                "/admin/fonts/**",
                                "/admin/images/**"
                        ).permitAll()

                        // 관리자 권한 검사
                        .anyRequest().hasRole("ADMIN")
                )

                .formLogin(form -> form
                        .loginPage("/admin/login")
                        .loginProcessingUrl("/admin/login")
                        .usernameParameter("userId")
                        .passwordParameter("userPw")
                        // 로그인 성공 후 항상 대시보드 이동
                        .defaultSuccessUrl("/admin/dashboard", true)
                        .failureUrl("/admin/login?error")
                        .permitAll()
                )

                .logout(logout -> logout
                        .logoutUrl("/admin/logout")
                        .logoutSuccessUrl("/admin/login?logout")
                        // 로그아웃 시 세션 무효화
                        .invalidateHttpSession(true)
                        .clearAuthentication(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )

                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint((request, response, error) -> {
                            if (isAdminApi(request)) {
                                writeJsonError(
                                        response, 401,
                                        "관리자 로그인이 필요합니다."
                                );
                            } else {
                                loginEntryPoint.commence(request, response, error);
                            }
                        })

                        .accessDeniedHandler((request, response, error) -> {
                            if (isAdminApi(request)) {
                                writeJsonError(
                                        response, 403,
                                        "접근 권한이 없거나 요청 검증에 실패했습니다."
                                );
                            } else {
                                response.sendError(
                                        HttpServletResponse.SC_FORBIDDEN,
                                        "접근 권한이 없거나 요청 검증에 실패했습니다."
                                );
                            }
                        })
                );

        return http.build();
    }

    private boolean isAdminApi(HttpServletRequest request) {
        String path = request.getServletPath();

        return path.equals("/api/admin")
                || path.startsWith("/api/admin/");
    }

    private void writeJsonError(
            HttpServletResponse response,
            int status,
            String message
    ) throws IOException {

        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.setHeader("Cache-Control", "no-store");

        response.getWriter().write(
                "{\"status\":" + status
                        + ",\"message\":\"" + message + "\"}"
        );
    }
}