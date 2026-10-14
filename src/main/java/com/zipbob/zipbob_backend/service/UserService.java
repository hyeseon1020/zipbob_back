package com.zipbob.zipbob_backend.service;

import com.zipbob.zipbob_backend.dto.UserLoginRequestDto;
import com.zipbob.zipbob_backend.dto.UserLoginResponseDto;
import com.zipbob.zipbob_backend.dto.UserSignupRequestDto;
import com.zipbob.zipbob_backend.dto.UserSignupResponseDto;
import com.zipbob.zipbob_backend.dto.UserMeResponseDto;
import com.zipbob.zipbob_backend.entity.User;
import com.zipbob.zipbob_backend.security.JwtTokenProvider;
import com.zipbob.zipbob_backend.repository.UserDeviceRepository;
import com.zipbob.zipbob_backend.repository.UserRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
// 회원가입
public class UserService {

    private final UserRepository userRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;

    // 영문 대소문자·숫자·_, 4~50자
    private static final Pattern USER_ID_PATTERN =
            Pattern.compile("[A-Za-z0-9_]{4,50}");

    // 한글 완성형·영문·숫자·_, 2~20자
    private static final Pattern USER_NAME_PATTERN =
            Pattern.compile("[가-힣A-Za-z0-9_]{2,20}");

    // 이메일 @ 앞부분. 영문 소문자·숫자·지정된 특수문자 허용, 점은 처음·끝·연속 사용 불가
    private static final Pattern EMAIL_LOCAL_PATTERN =
            Pattern.compile(
                    "[a-z0-9!#$%&'*+/=?^_`{|}~-]+"
                            + "(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*"
            );

    // 도메인 형식. 점으로 구분하고 각 부분의 처음·끝에 - 사용 불가
    private static final Pattern EMAIL_DOMAIN_PATTERN =
            Pattern.compile(
                    "[a-z0-9](?:[a-z0-9-]*[a-z0-9])?"
                            + "(?:\\.[a-z0-9](?:[a-z0-9-]*[a-z0-9])?)+"
            );

    // @Transcation : DB 작업을 하나의 트랜잭션으로 묶기, 중간에 예외가 발생하면 가입 저장을 취소
    @Transactional
    public UserSignupResponseDto signup(UserSignupRequestDto request) {

        if (request == null) {
            throw badRequest("회원가입 정보를 입력해 주세요.");
        }

        // 1. 필수 문자열 확인 및 앞뒤 공백 제거
        String userId = requiredText(
                request.getUserId(),
                "아이디를 입력해 주세요."
        );

        String userName = requiredText(
                request.getUserName(),
                "닉네임을 입력해 주세요."
        );

        String email = requiredText(
                request.getEmail(),
                "이메일 아이디를 입력해 주세요."
        ).toLowerCase(Locale.ROOT);

        String emailAdr = requiredText(
                request.getEmailAdr(),
                "이메일 도메인을 입력해 주세요."
        ).toLowerCase(Locale.ROOT);

        // 비밀번호는 공백 제거 없이 입력값 그대로 사용
        String userPw = request.getUserPw();

        // 2. 입력 형식 검사
        if (!USER_ID_PATTERN.matcher(userId).matches()) {
            throw badRequest(
                    "아이디는 영문, 숫자, 밑줄을 사용해 4~50자로 입력해 주세요."
            );
        }

        if (!USER_NAME_PATTERN.matcher(userName).matches()) {
            throw badRequest(
                    "닉네임은 한글, 영문, 숫자, 밑줄을 사용해 2~20자로 입력해 주세요."
            );
        }

        validatePassword(userPw);

        if (!userPw.equals(request.getUserPwConfirm())) {
            throw badRequest("비밀번호 확인이 일치하지 않습니다.");
        }

        if (email.length() > 50
                || !EMAIL_LOCAL_PATTERN.matcher(email).matches()) {
            throw badRequest("이메일 아이디 형식을 확인해 주세요.");
        }

        if (emailAdr.length() > 50
                || !EMAIL_DOMAIN_PATTERN.matcher(emailAdr).matches()) {
            throw badRequest("이메일 도메인 형식을 확인해 주세요.");
        }

        Integer userDeviceId = request.getUserDeviceId();

        if (userDeviceId == null || userDeviceId <= 0) {
            throw badRequest("등록된 기기 ID가 필요합니다.");
        }

        // 3. 비밀번호 해시
        // DB 잠금을 잡기 전에 처리해 잠금 시간을 줄임
        String encodedPassword = passwordEncoder.encode(userPw);

        // 4. 등록된 기기 확인 및 잠금
        // 같은 기기의 동시 가입 요청을 순서대로 처리
        userDeviceRepository.findByIdForUpdate(userDeviceId)
                .orElseThrow(() ->
                        badRequest("기기 등록 후 회원가입을 진행해 주세요.")
                );

        // 5. 아이디·닉네임·이메일·기기 중복 검사
        checkDuplicates(userId, userName, email, emailAdr, userDeviceId);

        // 6. 회원 INSERT
        int inserted = userRepository.insertUser(
                userId,
                userDeviceId,
                encodedPassword,
                userName,
                email,
                emailAdr
        );

        // 사전 검사 이후 발생한 DB 중복 충돌 처리
        if (inserted == 0) {
            checkDuplicates(
                    userId,
                    userName,
                    email,
                    emailAdr,
                    userDeviceId
            );

            throw conflict(
                    "가입 정보가 중복됩니다. 입력값을 확인해 주세요."
            );
        }

        // 7. 저장한 회원 조회 후 응답 DTO로 변환
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "가입한 회원 정보를 찾을 수 없습니다."
                        )
                );

        return new UserSignupResponseDto(user);
    }
    // 중복 검사
    private void checkDuplicates(
            String userId,
            String userName,
            String email,
            String emailAdr,
            Integer userDeviceId
    ) {
        if (userRepository.existsById(userId)) {
            throw conflict(
                    "이미 사용 중인 아이디입니다. 다른 아이디를 입력해 주세요."
            );
        }

        if (userRepository.existsByUserName(userName)) {
            throw conflict(
                    "중복된 닉네임입니다. 다른 닉네임을 입력해 주세요."
            );
        }

        if (userRepository.existsByEmailAndEmailAdr(email, emailAdr)) {
            throw conflict("이미 가입된 이메일입니다.");
        }

        if (userRepository.existsByUserDevice_UserDeviceId(userDeviceId)) {
            throw conflict("이 기기에는 이미 가입된 계정이 있습니다.");
        }
    }
    // 비밀번호 형식 검사
    private static void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw badRequest("비밀번호는 8자 이상 입력해 주세요.");
        }

        // BCrypt 입력 제한
        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw badRequest(
                    "비밀번호는 UTF-8 기준 72바이트 이하로 입력해 주세요."
            );
        }

        boolean containsLetter =
                Pattern.compile("[A-Za-z]").matcher(password).find();

        boolean containsNumber =
                Pattern.compile("[0-9]").matcher(password).find();

        if (!containsLetter || !containsNumber) {
            throw badRequest("비밀번호에 영문과 숫자를 포함해 주세요.");
        }
    }
    // 필수 입력 확인 + 앞뒤 공백 제거
    private static String requiredText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw badRequest(message);
        }

        return value.trim();
    }
    // HTTP 400 예외 객체 생성 (입력값이 잘못된 요청)
    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                message
        );
    }
    // HTTP 409 예외 객체 생성 (입력값이 잘못된 요청)
    private static ResponseStatusException conflict(String message) {
        return new ResponseStatusException(
                HttpStatus.CONFLICT,
                message
        );
    }

    // 로그인 검증 + 토큰 발급
    @Transactional
    public UserLoginResponseDto login(UserLoginRequestDto request) {

        if (request == null) {
            throw badRequest("로그인 정보를 입력해 주세요.");
        }

        // 1. 필수 입력 확인
        String userId = requiredText(
                request.getUserId(),
                "아이디를 입력해 주세요."
        );

        // 비밀번호는 공백 제거 없이 그대로 사용
        String userPw = request.getUserPw();

        if (userPw == null || userPw.isEmpty()) {
            throw badRequest("비밀번호를 입력해 주세요.");
        }

        if (userPw.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw badRequest(
                    "비밀번호는 UTF-8 기준 72바이트 이하로 입력해 주세요."
            );
        }

        // 2. 회원 조회
        User user = userRepository.findById(userId)
                .orElseThrow(UserService::loginFailed);

        // 3. 비밀번호 확인
        if (!passwordEncoder.matches(userPw, user.getUserPw())) {
            throw loginFailed();
        }

        // 4. 액세스 토큰 생성
        String accessToken = jwtTokenProvider.createAccessToken(
                user.getUserId()
        );

        // 5. 리프레시 토큰 생성 및 해시 저장
        String refreshToken = refreshTokenService.issue(user);

        // 6. 토큰 두 개와 회원 정보 반환
        return new UserLoginResponseDto(
                user,
                accessToken,
                refreshToken,
                jwtTokenProvider.getAccessTokenExpirationSeconds()
        );
    }

    // 아이디 없음·비밀번호 불일치는 같은 메시지로 응답
    private static ResponseStatusException loginFailed() {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "아이디 또는 비밀번호가 일치하지 않습니다."
        );
    }

    // 인증된 회원 정보 조회
    @Transactional(readOnly = true)
    public UserMeResponseDto getMe(String userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.UNAUTHORIZED,
                        "회원 정보를 찾을 수 없습니다. 다시 로그인해 주세요."
                ));

        return new UserMeResponseDto(user);
    }
}