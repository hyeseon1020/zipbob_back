package com.zipbob.zipbob_backend.service;

import com.zipbob.zipbob_backend.admin.dto.UserResponseDto;
import com.zipbob.zipbob_backend.dto.FindIdRequestDto;
import com.zipbob.zipbob_backend.dto.ResetPasswordRequestDto;
import com.zipbob.zipbob_backend.dto.UserLoginRequestDto;
import com.zipbob.zipbob_backend.dto.UserLoginResponseDto;
import com.zipbob.zipbob_backend.dto.UserMeResponseDto;
import com.zipbob.zipbob_backend.dto.UserSignupRequestDto;
import com.zipbob.zipbob_backend.dto.UserSignupResponseDto;
import com.zipbob.zipbob_backend.entity.User;
import com.zipbob.zipbob_backend.repository.UserDeviceRepository;
import com.zipbob.zipbob_backend.repository.UserRefreshTokenRepository;
import com.zipbob.zipbob_backend.repository.UserRepository;
import com.zipbob.zipbob_backend.security.JwtTokenProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final RefreshTokenService refreshTokenService;
    private final EmailVerificationService emailVerificationService;
    private final UserRefreshTokenRepository userRefreshTokenRepository;

    private static final Pattern USER_ID_PATTERN =
            Pattern.compile("[A-Za-z0-9_]{4,50}");

    private static final Pattern USER_NAME_PATTERN =
            Pattern.compile("[가-힣A-Za-z0-9_]{2,20}");

    private static final Pattern EMAIL_LOCAL_PATTERN =
            Pattern.compile(
                    "[a-z0-9!#$%&'*+/=?^_`{|}~-]+"
                            + "(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*"
            );

    private static final Pattern EMAIL_DOMAIN_PATTERN =
            Pattern.compile(
                    "[a-z0-9](?:[a-z0-9-]*[a-z0-9])?"
                            + "(?:\\.[a-z0-9](?:[a-z0-9-]*[a-z0-9])?)+"
            );

    // 회원가입
    @Transactional
    public UserSignupResponseDto signup(
            UserSignupRequestDto request
    ) {
        if (request == null) {
            throw badRequest("회원가입 정보를 입력해 주세요.");
        }

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

        String userPw = request.getUserPw();

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

        String encodedPassword = passwordEncoder.encode(userPw);

        // 가입 기기의 존재를 확인하고 트랜잭션 동안 잠금.
        // 같은 기기로 가입한 계정이 있어도 가입을 허용한다.
        userDeviceRepository.findByIdForUpdate(userDeviceId)
                .orElseThrow(() ->
                        badRequest("기기 등록 후 회원가입을 진행해 주세요.")
                );

        checkDuplicates(
                userId,
                userName,
                email,
                emailAdr
        );

        emailVerificationService.verifyAndUseForSignup(
                email,
                emailAdr,
                request.getVerificationToken()
        );

        int inserted = userRepository.insertUser(
                userId,
                userDeviceId,
                encodedPassword,
                userName,
                email,
                emailAdr
        );

        if (inserted == 0) {
            checkDuplicates(
                    userId,
                    userName,
                    email,
                    emailAdr
            );

            throw conflict(
                    "가입 정보가 중복됩니다. 입력값을 확인해 주세요."
            );
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new IllegalStateException(
                                "가입한 회원 정보를 찾을 수 없습니다."
                        )
                );

        return new UserSignupResponseDto(user);
    }

    // 로그인
    @Transactional
    public UserLoginResponseDto login(
            UserLoginRequestDto request
    ) {
        if (request == null) {
            throw badRequest("로그인 정보를 입력해 주세요.");
        }

        String userId = requiredText(
                request.getUserId(),
                "아이디를 입력해 주세요."
        );

        String userPw = request.getUserPw();

        if (userPw == null || userPw.isEmpty()) {
            throw badRequest("비밀번호를 입력해 주세요.");
        }

        if (userPw.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw badRequest(
                    "비밀번호는 UTF-8 기준 72바이트 이하로 입력해 주세요."
            );
        }

        User user = userRepository.findById(userId)
                .orElseThrow(UserService::loginFailed);

        if (!passwordEncoder.matches(userPw, user.getUserPw())) {
            throw loginFailed();
        }

        String accessToken =
                jwtTokenProvider.createAccessToken(user.getUserId());

        String refreshToken =
                refreshTokenService.issue(user);

        return new UserLoginResponseDto(
                user,
                accessToken,
                refreshToken,
                jwtTokenProvider.getAccessTokenExpirationSeconds()
        );
    }

    // 현재 회원 정보
    @Transactional(readOnly = true)
    public UserMeResponseDto getMe(String userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new ResponseStatusException(
                                HttpStatus.UNAUTHORIZED,
                                "회원 정보를 찾을 수 없습니다. 다시 로그인해 주세요."
                        )
                );

        return new UserMeResponseDto(user);
    }

    // 아이디 찾기
    @Transactional
    public String findId(FindIdRequestDto request) {
        if (request == null) {
            throw badRequest("아이디 찾기 정보를 입력해 주세요.");
        }

        String email = requiredText(
                request.getEmail(),
                "이메일을 입력해 주세요."
        ).toLowerCase(Locale.ROOT);

        String emailAdr = requiredText(
                request.getEmailAdr(),
                "이메일 도메인을 입력해 주세요."
        ).toLowerCase(Locale.ROOT);

        emailVerificationService.verifyAndUse(
                email,
                emailAdr,
                request.getVerificationToken(),
                "FIND_ID"
        );

        User user = userRepository
                .findByEmailAndEmailAdr(email, emailAdr)
                .orElseThrow(() ->
                        badRequest("일치하는 회원 정보가 없습니다.")
                );

        return user.getUserId();
    }

    // 비밀번호 재설정
    @Transactional
    public void resetPassword(
            ResetPasswordRequestDto request
    ) {
        if (request == null) {
            throw badRequest("비밀번호 재설정 정보를 입력해 주세요.");
        }

        String userId = requiredText(
                request.getUserId(),
                "아이디를 입력해 주세요."
        );

        String email = requiredText(
                request.getEmail(),
                "이메일을 입력해 주세요."
        ).toLowerCase(Locale.ROOT);

        String emailAdr = requiredText(
                request.getEmailAdr(),
                "이메일 도메인을 입력해 주세요."
        ).toLowerCase(Locale.ROOT);

        validatePassword(request.getUserPw());

        if (!request.getUserPw().equals(request.getUserPwConfirm())) {
            throw badRequest("비밀번호 확인이 일치하지 않습니다.");
        }

        String encodedPassword =
                passwordEncoder.encode(request.getUserPw());

        emailVerificationService.verifyAndUse(
                email,
                emailAdr,
                request.getVerificationToken(),
                "RESET_PASSWORD"
        );

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        badRequest("일치하는 회원 정보가 없습니다.")
                );

        if (!email.equals(user.getEmail())
                || !emailAdr.equals(user.getEmailAdr())) {
            throw badRequest("일치하는 회원 정보가 없습니다.");
        }

        user.changePassword(encodedPassword);

        // 기존의 모든 리프레시 토큰 폐기
        userRefreshTokenRepository.deleteAllByUserId(userId);
    }

    // 기기 중복은 검사하지 않는다.
    private void checkDuplicates(
            String userId,
            String userName,
            String email,
            String emailAdr
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
    }

    private static void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw badRequest("비밀번호는 8자 이상 입력해 주세요.");
        }

        if (password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw badRequest(
                    "비밀번호는 UTF-8 기준 72바이트 이하로 입력해 주세요."
            );
        }

        boolean containsLetter =
                Pattern.compile("[A-Za-z]")
                        .matcher(password)
                        .find();

        boolean containsNumber =
                Pattern.compile("[0-9]")
                        .matcher(password)
                        .find();

        if (!containsLetter || !containsNumber) {
            throw badRequest(
                    "비밀번호에 영문과 숫자를 포함해 주세요."
            );
        }
    }

    private static String requiredText(
            String value,
            String message
    ) {
        if (value == null || value.isBlank()) {
            throw badRequest(message);
        }

        return value.trim();
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                message
        );
    }

    private static ResponseStatusException conflict(String message) {
        return new ResponseStatusException(
                HttpStatus.CONFLICT,
                message
        );
    }

    private static ResponseStatusException loginFailed() {
        return new ResponseStatusException(
                HttpStatus.UNAUTHORIZED,
                "아이디 또는 비밀번호가 일치하지 않습니다."
        );
    }

    /*
    * 관리자 페이지
    * */
    public List<UserResponseDto> getAllUsers() {
        // DB에서 회원 목록을 조회하여 DTO로 변환 후 반환
        return userRepository.findAll().stream()
                .map(user -> {
                    UserResponseDto dto = new UserResponseDto();
                    dto.setUserId(user.getUserId());
                    if (user.getUserDevice() != null) {
                        dto.setUserDevice(user.getUserDevice().getUserDeviceId());
                    }
                    dto.setUserName(user.getUserName());
                    dto.setEmail(user.getEmail());
                    dto.setEmailAdr(user.getEmailAdr());
                    dto.setUserRole(user.getUserRole());
                    return dto;
                })
                .collect(Collectors.toList());
    }
}