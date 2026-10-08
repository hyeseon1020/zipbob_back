package com.zipbob.zipbob_backend.dto;

import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
// 앱에서 보내는 기기 등록 요청 데이터
public class UserDeviceRequestDto {
    // 앱 최초 실행 시 생성하고 저장한 UUID
    private String deviceId;
    // 실행 플랫폼: ANDROID, IOS, WEB
    private String osType;
    // OS 버전: 조회하지 못하면 NULL 허용
    private String osVersion;
    // 기기 모델명: 조회하지 못하면 NULL 허용
    private String deviceModel;

    // 요청 DTO: 입력값으로 생성
    public UserDeviceRequestDto(
            String deviceId,
            String osType,
            String osVersion,
            String deviceModel
    ) {
        this.deviceId = deviceId;
        this.osType = osType;
        this.osVersion = osVersion;
        this.deviceModel = deviceModel;
    }
}
