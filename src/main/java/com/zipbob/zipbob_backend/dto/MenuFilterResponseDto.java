package com.zipbob.zipbob_backend.dto;

import lombok.Getter;
import java.util.ArrayList;
import java.util.List;

@Getter
public class MenuFilterResponseDto {
    private final Integer menuId;
    private final String menuName;
    private final String category;
    private final String categoryName;
    private final String description;
    private final String imageUrl;
    private final List<TagDto> tags; // 태그들을 리스트로 관리

    public MenuFilterResponseDto(MenuListProjection p) {
        this.menuId = p.getMenuId();
        this.menuName = p.getMenuName();
        this.category = p.getCategory();
        this.categoryName = p.getCategoryName();
        this.description = p.getDescription();
        this.imageUrl = p.getImageUrl();
        this.tags = new ArrayList<>();
    }

    // 태그 정보를 담을 내부 DTO
    @Getter
    public static class TagDto {
        private final String tagSeq;
        private final String codeId;
        private final String tagName;

        public TagDto(MenuListProjection p) {
            this.tagSeq = p.getTagSeq();
            this.codeId = p.getCodeId();
            this.tagName = p.getTagName();
        }
    }
}