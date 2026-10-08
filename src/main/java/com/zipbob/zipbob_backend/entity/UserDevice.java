package com.zipbob.zipbob_backend.entity;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_device_mng", schema = "zipbob_app")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserDevice {

    @Id
    // DB에서 생성하는 자동 증가 번호 사용
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "user_device_id")
    private Integer userDeviceId;

    // Flutter에서 생성하고 저장한 UUID
    @Column(name = "device_id", nullable = false, length = 255)
    private String deviceId;

    @Column(name = "fcm_token", columnDefinition = "text")
    private String fcmToken;

    @Column(name = "os_type", length = 20)
    private String osType;

    @Column(name = "os_version", length = 20)
    private String osVersion;

    @Column(name = "device_model", length = 100)
    private String deviceModel;

    @Column(name = "crated_dt", insertable = false, updatable = false)
    private LocalDateTime createdDt;

    @Column(name = "updated_dt")
    private LocalDateTime updatedDt;
}

