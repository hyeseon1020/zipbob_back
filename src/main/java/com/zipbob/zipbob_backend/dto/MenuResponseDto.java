package com.zipbob.zipbob_backend.dto;

import com.zipbob.zipbob_backend.entity.Menu;
import com.zipbob.zipbob_backend.entity.MenuFile;
import lombok.Getter;
// 메뉴 레시피 조회 (메뉴 정보·이미지·인분·시간·난이도·재료·조리 순서)
@Getter
public class MenuResponseDto {

    private final Integer menuId;
    private final String menuName;
    private final String category;
    private final String description;
    private final Integer servings;
    private final Integer cookTime;
    private final Integer cookMinute;
    private final String ingredients;
    private final String steps;
    private final String difficultyLvl;
    private final String imageUrl;

    public MenuResponseDto(Menu menu) {
        this.menuId = menu.getMenuId();
        this.menuName = menu.getMenuName();
        this.category = menu.getCategory();
        this.description = menu.getDescription();
        this.servings = menu.getServings();
        this.cookTime = menu.getCookTime();
        this.cookMinute = menu.getCookMinute();
        this.ingredients = menu.getIngredients();
        this.steps = menu.getSteps();
        this.difficultyLvl = menu.getDifficultyLvl();

        this.imageUrl = menu.getFiles().stream()
                .filter(file -> "Y".equals(file.getUseYn()))
                .map(MenuFile::getFilePath)
                .findFirst()
                .orElse(null);
    }
}