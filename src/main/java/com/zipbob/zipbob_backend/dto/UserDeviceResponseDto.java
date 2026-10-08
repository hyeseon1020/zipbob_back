package com.zipbob.zipbob_backend.dto;

import com.zipbob.zipbob_backend.entity.UserDevice;
import lombok.Getter;

// 프론트에 보내는 기기 등록 결과
@Getter
public class UserDeviceResponseDto {

    private final Integer userDeviceId;
    private final String deviceId;
    private final String osType;
    private final String osVersion;
    private final String deviceModel;

    // 등록·조회한 엔티티의 값을 응답 DTO에 저장
    public UserDeviceResponseDto(UserDevice userDevice) {
        this.userDeviceId = userDevice.getUserDeviceId();
        this.deviceId = userDevice.getDeviceId();
        this.osType = userDevice.getOsType();
        this.osVersion = userDevice.getOsVersion();
        this.deviceModel = userDevice.getDeviceModel();
    }
}