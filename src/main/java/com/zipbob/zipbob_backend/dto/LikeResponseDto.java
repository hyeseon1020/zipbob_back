package com.zipbob.zipbob_backend.dto;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
// 즐겨찾기 목록 조회 결과를 프론트 응답 형태로 변환
public class LikeResponseDto {

    private final Integer menuId;
    private final String menuName;
    private final String category;
    private final String categoryName;
    private final String description;
    private final String imageUrl;
    private final LocalDateTime createdDt;

    public LikeResponseDto(LikeListProjection like) {
        this.menuId = like.getMenuId();
        this.menuName = like.getMenuName();
        this.category = like.getCategory();
        this.categoryName = like.getCategoryName();
        this.description = like.getDescription();
        this.imageUrl = like.getImageUrl();
        this.createdDt = like.getCreatedDt();
    }
}