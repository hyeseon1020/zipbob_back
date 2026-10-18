package com.zipbob.zipbob_backend.security;

import com.zipbob.zipbob_backend.repository.UserRepository;

import io.jsonwebtoken.JwtException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.Set;

// 요청 헤더의 토큰을 검증하고, 확인된 회원 아이디를 Spring Security에 등록
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final AuthenticationEntryPoint authenticationEntryPoint;

    private static final Set<String> PUBLIC_AUTH_PATHS = Set.of(
            "/api/users/signup",
            "/api/users/login",
            "/api/users/refresh",
            "/api/users/logout",
            "/api/email-verifications/send",
            "/api/email-verifications/confirm"
    );

    public JwtAuthenticationFilter(
            JwtTokenProvider jwtTokenProvider,
            UserRepository userRepository,
            AuthenticationEntryPoint authenticationEntryPoint
    ) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
        this.authenticationEntryPoint = authenticationEntryPoint;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {

        // CORS 사전 요청
        if ("OPTIONS".equals(request.getMethod())) {
            return true;
        }

        // 로그인·재발급·로그아웃은 액세스 토큰 없이 호출
        return "POST".equals(request.getMethod())
                && PUBLIC_AUTH_PATHS.contains(request.getServletPath());
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {

        String authorization =
                request.getHeader(HttpHeaders.AUTHORIZATION);

        // 토큰이 없으면 다음 필터에서 접근 권한 판단
        if (authorization == null) {
            filterChain.doFilter(request, response);
            return;
        }

        // Authorization: Bearer 토큰
        if (!authorization.regionMatches(
                true, 0, "Bearer ", 0, 7
        )) {
            reject(request, response);
            return;
        }

        String accessToken = authorization.substring(7);
        String userId;

        // 토큰 검증에서 발생한 오류만 인증 실패로 처리
        try {
            userId = jwtTokenProvider.validateAndGetUserId(accessToken);
        } catch (JwtException | IllegalArgumentException exception) {
            reject(request, response);
            return;
        }

        // 삭제된 회원의 토큰도 거부
        if (!userRepository.existsById(userId)) {
            reject(request, response);
            return;
        }

        // 현재 회원은 일반 회원 권한으로 등록
        UsernamePasswordAuthenticationToken authentication =
                UsernamePasswordAuthenticationToken.authenticated(
                        userId,
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_USER"))
                );

        SecurityContext context =
                SecurityContextHolder.createEmptyContext();

        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        filterChain.doFilter(request, response);
    }

    private void reject(
            HttpServletRequest request,
            HttpServletResponse response
    ) throws ServletException, IOException {

        SecurityContextHolder.clearContext();

        authenticationEntryPoint.commence(
                request,
                response,
                new BadCredentialsException("유효하지 않은 액세스 토큰")
        );
    }
}