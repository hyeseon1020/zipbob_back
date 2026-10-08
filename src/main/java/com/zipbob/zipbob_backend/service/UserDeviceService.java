package com.zipbob.zipbob_backend.service;

import com.zipbob.zipbob_backend.dto.UserDeviceRequestDto;
import com.zipbob.zipbob_backend.dto.UserDeviceResponseDto;
import com.zipbob.zipbob_backend.entity.UserDevice;
import com.zipbob.zipbob_backend.repository.UserDeviceRepository;

import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
// UUID·OS 검사, 부가정보 공백 제거·길이 정리
public class UserDeviceService {

    // Repository 주입
    private final UserDeviceRepository userDeviceRepository;

    // UUID 문자열 형식 검사: 8-4-4-4-12
    private static final Pattern UUID_PATTERN = Pattern.compile(
            "[0-9a-fA-F]{8}-"
                    + "[0-9a-fA-F]{4}-"
                    + "[0-9a-fA-F]{4}-"
                    + "[0-9a-fA-F]{4}-"
                    + "[0-9a-fA-F]{12}"
    );

    // 현재 지원하는 실행 플랫폼
    private static final Set<String> OS_TYPES = Set.of(
            "ANDROID",
            "IOS",
            "WEB"
    );

    // 등록과 조회를 한 트랜잭션으로 처리
    @Transactional
    public UserDeviceResponseDto register(
            UserDeviceRequestDto request
    ) {

        // 1. UUID 필수값 및 형식 검사
        if (request == null
                || request.getDeviceId() == null
                || !UUID_PATTERN.matcher(request.getDeviceId()).matches()) {

            throw badRequest("deviceId는 UUID 형식이어야 합니다.");
        }

        // 2. OS 구분 검사
        if (request.getOsType() == null
                || !OS_TYPES.contains(request.getOsType())) {

            throw badRequest(
                    "osType은 ANDROID, IOS, WEB 중 하나여야 합니다."
            );
        }

        // 3. 저장할 데이터 정리
        // UUID는 소문자로 통일
        // OS 버전·모델명은 없으면 NULL, 길이가 넘으면 잘라서 저장
        UserDeviceRequestDto normalized =
                new UserDeviceRequestDto(
                        request.getDeviceId().toLowerCase(Locale.ROOT),
                        request.getOsType(),
                        normalizeOptional(
                                request.getOsVersion(),
                                20
                        ),
                        normalizeOptional(
                                request.getDeviceModel(),
                                100
                        )
                );

        // 4. 새로운 UUID면 등록, 기존 UUID면 정보 갱신
        userDeviceRepository.registerDevice(
                normalized.getDeviceId(),
                normalized.getOsType(),
                normalized.getOsVersion(),
                normalized.getDeviceModel()
        );

        // 5. 등록·갱신한 기기를 UUID로 조회
        UserDevice userDevice = userDeviceRepository
                .findByDeviceId(normalized.getDeviceId())
                .orElseThrow(() ->
                        new IllegalStateException(
                                "등록한 기기 정보를 찾을 수 없습니다."
                        )
                );

        // 6. 엔티티를 응답 DTO로 변환
        return new UserDeviceResponseDto(userDevice);
    }

    // 앱이 자동 조회한 부가정보 정리
    // 값이 없으면 NULL, 앞뒤 공백 제거, 최대 길이를 넘으면 잘라서 등록 실패를 방지
    private static String normalizeOptional(
            String value,
            int maxLength
    ) {

        if (value == null || value.isBlank()) {
            return null;
        }

        String normalized = value.trim();

        if (normalized.length() > maxLength) {
            return normalized.substring(0, maxLength);
        }

        return normalized;
    }

    // UUID·OS 구분 등 필수 데이터가 잘못되면 HTTP 400으로 처리
    private static ResponseStatusException badRequest(
            String message
    ) {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                message
        );
    }
}