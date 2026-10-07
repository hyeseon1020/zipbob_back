package com.zipbob.zipbob_backend.dto;

import com.zipbob.zipbob_backend.entity.Menu;
import com.zipbob.zipbob_backend.entity.MenuFile;
import lombok.Getter;
// 전체 메뉴 (ID·메뉴명·카테고리·설명·이미지)
@Getter
public class MenuListResponseDto {
    // 프론트에 보내는 목록 데이터의 형태
    private final Integer menuId;
    private final String menuName;
    private final String category;
    private final String categoryName;
    private final String description;
    private final String imageUrl;

    // MenuListProjection에서 읽은 값을 그대로 DTO 필드에 저장
    public MenuListResponseDto(MenuListProjection menu) {
        this.menuId = menu.getMenuId();
        this.menuName = menu.getMenuName();
        this.category = menu.getCategory();
        this.categoryName = menu.getCategoryName();
        this.description = menu.getDescription();
        this.imageUrl = menu.getImageUrl();
    }
}