package com.zipbob.zipbob_backend.dto;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class RecentRecipeResponseDto {

    private final Integer menuId;
    private final String menuName;
    private final String category;
    private final String categoryName;
    private final String description;
    private final String imageUrl;
    private final LocalDateTime viewedDt;

    // Repository 조회 결과를 API 응답 DTO로 변환
    public RecentRecipeResponseDto(
            RecentRecipeListProjection projection
    ) {
        this.menuId = projection.getMenuId();
        this.menuName = projection.getMenuName();
        this.category = projection.getCategory();
        this.categoryName = projection.getCategoryName();
        this.description = projection.getDescription();
        this.imageUrl = projection.getImageUrl();
        this.viewedDt = projection.getViewedDt();
    }
}