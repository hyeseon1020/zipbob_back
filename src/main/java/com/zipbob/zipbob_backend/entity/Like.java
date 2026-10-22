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

    // 회원 ID + 메뉴 ID 복합키
    @EmbeddedId
    private LikeId id;

    @Column(name = "created_id", length = 50)
    private String createdId;

    // INSERT 시 DB의 CURRENT_TIMESTAMP 기본값 사용
    @Column(name = "crated_dt", insertable = false, updatable = false)
    private LocalDateTime createdDt;

    public Like(String userId, Integer menuId, String createdId) {
        this.id = new LikeId(userId, menuId);
        this.createdId = createdId;
    }
}