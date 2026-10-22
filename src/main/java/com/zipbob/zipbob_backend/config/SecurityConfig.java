package com.zipbob.zipbob_backend.config;

import com.zipbob.zipbob_backend.repository.UserRepository;
import com.zipbob.zipbob_backend.security.JwtAuthenticationFilter;
import com.zipbob.zipbob_backend.security.JwtTokenProvider;

import jakarta.servlet.DispatcherType;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    public AuthenticationEntryPoint authenticationEntryPoint() {
        return (request, response, exception) -> {
            response.setStatus(401);
            response.setCharacterEncoding("UTF-8");
            response.setContentType("application/json");
            response.setHeader("WWW-Authenticate", "Bearer");
            response.setHeader("Cache-Control", "no-store");

            response.getWriter().write("""
                {
                  "status": 401,
                  "message": "로그인이 필요하거나 액세스 토큰이 유효하지 않습니다."
                }
                """);
        };
    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtTokenProvider jwtTokenProvider,
            UserRepository userRepository,
            AuthenticationEntryPoint authenticationEntryPoint
    ) throws Exception {

        JwtAuthenticationFilter jwtFilter =
                new JwtAuthenticationFilter(
                        jwtTokenProvider,
                        userRepository,
                        authenticationEntryPoint
                );

        http
                // 기존 WebConfig의 CORS 설정 사용
                .cors(Customizer.withDefaults())

                // Authorization 헤더로 인증하는 API
                .csrf(csrf -> csrf.disable())

                .sessionManagement(session -> session
                        .sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .requestCache(cache -> cache.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())
                .logout(logout -> logout.disable())

                .exceptionHandling(exception -> exception
                        .authenticationEntryPoint(
                                authenticationEntryPoint
                        )
                        .accessDeniedHandler((request, response, error) -> {
                            response.setStatus(403);
                            response.setCharacterEncoding("UTF-8");
                            response.setContentType("application/json");
                            response.setHeader("Cache-Control", "no-store");

                            response.getWriter().write("""
                                {
                                  "status": 403,
                                  "message": "접근 권한이 없습니다."
                                }
                                """);
                        })
                )

                .authorizeHttpRequests(auth -> auth
                        // 오류 처리 요청 허용
                        .dispatcherTypeMatchers(
                                DispatcherType.ERROR
                        ).permitAll()

                        // CORS 사전 요청 허용
                        .requestMatchers(
                                HttpMethod.OPTIONS,
                                "/**"
                        ).permitAll()

                        // 관리자 HTML 화면 확인용 임시 허용
                        // 관리자 인증 구현 시 관리자 권한 검사로 변경
                        .requestMatchers(
                                HttpMethod.GET,
                                "/admin",
                                "/admin/**"
                        ).permitAll()

                        // 정적 리소스
                        .requestMatchers(
                                HttpMethod.GET,
                                "/css/**",
                                "/js/**",
                                "/fonts/**",
                                "/images/**",
                                "/favicon.ico"
                        ).permitAll()

                        // 로그인 없이 사용하는 회원·인증 API
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/users/signup",
                                "/api/users/login",
                                "/api/users/refresh",
                                "/api/users/logout",
                                "/api/users/find-id",
                                "/api/users/reset-password",
                                "/api/email-verifications/send",
                                "/api/email-verifications/confirm"
                        ).permitAll()

                        // 회원 전용 조회 API
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/users/me",
                                "/api/favorites",
                                "/api/recent-recipes"
                        ).authenticated()

                        // 회원 전용 수정·등록 API
                        // 닉네임 변경, 즐겨찾기 등록, 최근 본 레시피 기록
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/users/me/nickname",
                                "/api/favorites/*",
                                "/api/recent-recipes/*"
                        ).authenticated()

                        // 회원 전용 삭제 API
                        // 즐겨찾기 해제
                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/favorites/*"
                        ).authenticated()

                        // 비회원 조회 API
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/menus",
                                "/api/menus/**",
                                "/api/codes/**",
                                "/api/menufilter",
                                "/api/test"
                        ).permitAll()

                        // 기기 등록·조건 추천
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/user-devices",
                                "/api/menufilter/recommend"
                        ).permitAll()

                        // 위에서 허용하지 않은 요청은 차단
                        .anyRequest().denyAll()
                )

                .addFilterBefore(
                        jwtFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}