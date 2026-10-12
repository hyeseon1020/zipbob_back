package com.zipbob.zipbob_backend.repository;

import com.zipbob.zipbob_backend.entity.UserDevice;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

// 관리할 엔티티: UserDevice, PK 타입: Integer
public interface UserDeviceRepository
        extends JpaRepository<UserDevice, Integer> {

    // UUID로 등록된 기기 조회
    Optional<UserDevice> findByDeviceId(String deviceId);

    /*
     * 기기 최초 등록 또는 기존 기기 정보 갱신
     *
     * 새로운 UUID: INSERT
     * 기존 UUID: UPDATE
     *
     * 기존 기기의 PK, 생성일, 푸시 토큰은 유지
     */
    @Modifying
    @Query(value = """
        INSERT INTO zipbob_app.user_device_mng AS d
            (
                device_id,
                fcm_token,
                os_type,
                os_version,
                device_model
            )
        VALUES
            (
                :deviceId,
                NULL,
                :osType,
                :osVersion,
                :deviceModel
            )
        ON CONFLICT (device_id)
        DO UPDATE SET
            os_type = EXCLUDED.os_type,
            os_version = COALESCE(
                EXCLUDED.os_version,
                d.os_version
            ),
            device_model = COALESCE(
                EXCLUDED.device_model,
                d.device_model
            ),
            updated_dt = CURRENT_TIMESTAMP
        """, nativeQuery = true)
    int registerDevice(
            @Param("deviceId") String deviceId,
            @Param("osType") String osType,
            @Param("osVersion") String osVersion,
            @Param("deviceModel") String deviceModel
    );

    // 같은 기기의 최근 본 기록 저장 요청을 순서대로 처리하기 위한 잠금
    // JPQL : 테이블명이 아닌 Entity와 Java 필드명 사용
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
        SELECT u
        FROM UserDevice u
        WHERE u.userDeviceId = :userDeviceId
        """)
    Optional<UserDevice> findByIdForUpdate(
            @Param("userDeviceId") Integer userDeviceId
    );
}