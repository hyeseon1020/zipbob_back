package com.zipbob.zipbob_backend.dto;

import lombok.Getter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Getter
@ToString
public class MenuFilterResponseDto {
    private final Integer menuId;
    private final String menuName;
    private final String category;
    private final String categoryName;
    private final String description;
    private String imageUrl;
    private final List<TagDto> tags = new ArrayList<>(); // 태그들을 리스트로 관리

    // 1. 기존 전체 조회용 (MenuTagListProjection 받는 생성자)
    public MenuFilterResponseDto(MenuTagListProjection p) {
        this.menuId = p.getMenuId();
        this.menuName = p.getMenuName();
        this.category = p.getCategory();
        this.categoryName = p.getCategoryName();
        this.description = p.getDescription();
        //이중 배열
    }

    // 2. [추가] 필터링 조회용 (MenuListProjection 받는 생성자)com.zipbob.zipbob_backend.dto.MenuFilterResponseDto
    public MenuFilterResponseDto(MenuListProjection p) {
        this.menuId = p.getMenuId();
        this.menuName = p.getMenuName();
        this.category = p.getCategory();
        this.categoryName = p.getCategoryName();
        this.description = p.getDescription();
        this.imageUrl = p.getImageUrl();
        // tags는 필터링 조회 시 비어있거나 필요 없으므로 초기화 상태 유지
    }

    // 태그 정보를 담을 내부 DTO
    @Getter
    public static class TagDto {//이중 배열 안 리스트
        private final String tagSeq;
        private final String codeId;
        private final String tagName;

        public TagDto(MenuTagListProjection p) {
            this.tagSeq = p.getTagSeq();
            this.codeId = p.getCodeId();
            this.tagName = p.getTagName();
        }
    }
}