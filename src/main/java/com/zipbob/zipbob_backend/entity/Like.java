package com.zipbob.zipbob_backend.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "like_mng", schema = "zipbob_app")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Like {

    // 사용자 기기 ID + 메뉴 ID를 기본 키로 사용
    @EmbeddedId
    private LikeId id;

    @Column(name = "created_id", length = 50)
    private String createdId;

    // INSERT 시 DB의 CURRENT_TIMESTAMP 기본값 사용
    @Column(name = "crated_dt", insertable = false, updatable = false)
    private LocalDateTime createdDt;

    public Like(Integer userDeviceId, Integer menuId, String createdId) {
        this.id = new LikeId(userDeviceId, menuId);
        this.createdId = createdId;
    }
}