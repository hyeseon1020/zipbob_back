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
// 새 API를 추가하면 이 접근 규칙도 함께 수정
// 비회원·회원에 따라 호출할 수 있는 API를 구분
public class SecurityConfig {

    // 토큰 누락·만료·변조 시 JSON으로 401 반환
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
                // 기존 WebConfig의 MVC CORS 설정 사용
                .cors(Customizer.withDefaults())

                // 쿠키 인증 없이 Authorization 헤더로 인증하는 API
                .csrf(csrf -> csrf.disable())

                // 로그인 세션을 생성하거나 인증에 사용하지 않음
                .sessionManagement(session -> session
                        .sessionCreationPolicy(
                                SessionCreationPolicy.STATELESS
                        )
                )

                .requestCache(cache -> cache.disable())
                .formLogin(form -> form.disable())
                .httpBasic(basic -> basic.disable())

                // 직접 만든 /api/users/logout API 사용
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
                        // 오류 응답을 처리하는 내부 디스패치 허용
                        .dispatcherTypeMatchers(DispatcherType.ERROR)
                        .permitAll()

                        .requestMatchers(HttpMethod.OPTIONS, "/**")
                        .permitAll()

                        // 회원가입·로그인·재발급·로그아웃·이메일 인증
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/users/signup",
                                "/api/users/login",
                                "/api/users/refresh",
                                "/api/users/logout",
                                "/api/email-verifications/send",
                                "/api/email-verifications/confirm"
                        ).permitAll()

                        // 회원 전용
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/users/me"
                        ).authenticated()

                        // 비회원 조회 기능
                        .requestMatchers(
                                HttpMethod.GET,
                                "/api/menus",
                                "/api/menus/**",
                                "/api/codes/**",
                                "/api/menufilter",
                                "/api/favorites",
                                "/api/recent-recipes",
                                "/api/test",
                                "/images/**",
                                "/favicon.ico"
                        ).permitAll()

                        // 기기 등록·조건 추천
                        .requestMatchers(
                                HttpMethod.POST,
                                "/api/user-devices",
                                "/api/menufilter/recommend"
                        ).permitAll()

                        // 기기 기반 즐겨찾기·최근 본 레시피
                        .requestMatchers(
                                HttpMethod.PUT,
                                "/api/favorites/*",
                                "/api/recent-recipes/*"
                        ).permitAll()

                        .requestMatchers(
                                HttpMethod.DELETE,
                                "/api/favorites/*"
                        ).permitAll()

                        // 명시하지 않은 경로는 차단
                        .anyRequest().denyAll()
                )

                .addFilterBefore(
                        jwtFilter,
                        UsernamePasswordAuthenticationFilter.class
                );

        return http.build();
    }
}