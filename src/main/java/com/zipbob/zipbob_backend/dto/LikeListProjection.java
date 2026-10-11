package com.zipbob.zipbob_backend.dto;

import java.time.LocalDateTime;

// 즐겨찾기 목록 SQL 조회 결과를 읽는 인터페이스
public interface LikeListProjection {

    Integer getMenuId();

    String getMenuName();

    String getCategory();

    String getCategoryName();

    String getDescription();

    String getImageUrl();

    LocalDateTime getCreatedDt();
}