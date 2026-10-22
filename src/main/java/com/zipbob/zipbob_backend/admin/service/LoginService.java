package com.zipbob.zipbob_backend.admin.service;

import com.zipbob.zipbob_backend.entity.User;
import com.zipbob.zipbob_backend.repository.UserRepository;

import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
// 관리자 계정 조회
@Service
public class LoginService implements UserDetailsService {

    private final UserRepository userRepository;

    public LoginService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username)
            throws UsernameNotFoundException {

        User user = userRepository.findById(username)
                // 일반 회원이면 여기서 제외
                .filter(User::isAdmin)
                .orElseThrow(() -> new UsernameNotFoundException(
                        "아이디 또는 비밀번호를 확인해 주세요."
                ));

        return org.springframework.security.core.userdetails.User
                .withUsername(user.getUserId())
                .password(user.getUserPw())
                .roles("ADMIN")
                .build();
    }
}