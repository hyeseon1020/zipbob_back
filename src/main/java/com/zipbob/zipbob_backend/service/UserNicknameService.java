package com.zipbob.zipbob_backend.service;

import com.zipbob.zipbob_backend.dto.UserMeResponseDto;
import com.zipbob.zipbob_backend.dto.UserNicknameUpdateRequestDto;
import com.zipbob.zipbob_backend.entity.User;
import com.zipbob.zipbob_backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class UserNicknameService {

    private final UserRepository userRepository;

    // 회원가입과 동일한 닉네임 규칙
    // 한글 완성형·영문·숫자·밑줄, 2~20자
    private static final Pattern USER_NAME_PATTERN =
            Pattern.compile("[가-힣A-Za-z0-9_]{2,20}");

    @Transactional
    public UserMeResponseDto updateNickname(
            String userId,
            UserNicknameUpdateRequestDto request
    ) {
        // 1. 요청값 확인
        if (request == null
                || request.getUserName() == null
                || request.getUserName().isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "닉네임을 입력해 주세요."
            );
        }

        // 앞뒤 공백 제거
        String userName = request.getUserName().trim();

        // 2. 닉네임 형식 검사
        if (!USER_NAME_PATTERN.matcher(userName).matches()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "닉네임은 한글, 영문, 숫자, 밑줄을 사용해 2~20자로 입력해 주세요."
            );
        }

        // 3. 로그인한 회원 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "회원 정보를 찾을 수 없습니다. 다시 로그인해 주세요."
                        )
                );

        // 4. 현재 닉네임과 같으면 그대로 반환
        if (userName.equals(user.getUserName())) {
            return new UserMeResponseDto(user);
        }

        // 5. 다른 회원이 사용하는 닉네임인지 확인
        if (userRepository.existsByUserName(userName)) {
            throw new ResponseStatusException(
                    HttpStatus.CONFLICT,
                    "이미 사용 중인 닉네임입니다. 다른 닉네임을 입력해 주세요."
            );
        }

        // 6. 엔티티의 닉네임 변경
        user.changeNickname(userName);

        // 7. SQL을 실행해 DB 제약조건까지 확인
        userRepository.saveAndFlush(user);

        // 8. 변경된 회원 정보 반환
        return new UserMeResponseDto(user);
    }
}