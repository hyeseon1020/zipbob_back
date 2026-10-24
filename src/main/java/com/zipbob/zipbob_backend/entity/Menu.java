package com.zipbob.zipbob_backend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "menu_mng", schema = "zipbob_app")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Menu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "menu_id")
    private Integer menuId;

    @Column(name = "menu_name", nullable = false, length = 50)
    private String menuName;

    @Column(name = "category", length = 50)
    private String category;

    @Column(name = "description", length = 100)
    private String description;

    @Column(name = "servings")
    private Integer servings;

    // 조리 시간: 시간 단위
    @Column(name = "cook_time")
    private Integer cookTime;

    // 조리 시간: 분 단위
    @Column(name = "cook_minute")
    private Integer cookMinute;

    @Column(name = "ingredients", columnDefinition = "text")
    private String ingredients;

    @Column(name = "steps", columnDefinition = "text")
    private String steps;

    @Column(name = "created_id", length = 50)
    private String createdId;

    // DB의 실제 컬럼명이 crated_dt이므로 그대로 매핑
    // INSERT 시 DB의 CURRENT_TIMESTAMP 기본값 사용
    @Column(name = "crated_dt", insertable = false, updatable = false)
    private LocalDateTime createdDt;

    @Column(name = "updated_id", length = 50)
    private String updatedId;

    @Column(name = "updated_dt")
    private LocalDateTime updatedDt;

    @Column(name = "difficulty_lvl", length = 50)
    private String difficultyLvl;

    @OneToMany(mappedBy = "menu", fetch = FetchType.LAZY)
    @OrderBy("fileId ASC")
    private List<MenuFile> files = new ArrayList<>();

    @OneToMany(mappedBy = "menu", fetch = FetchType.LAZY)
    @OrderBy("codeId ASC")
    private List<Tag> tags = new ArrayList<>();

    // 메뉴 생성 메서드
    public static Menu create(
            String menuName,
            String category,
            String description,
            Integer servings,
            Integer cookTime,
            Integer cookMinute,
            String ingredients,
            String steps,
            String difficultyLvl,
            String adminId
    ) {
        Menu menu = new Menu();

        menu.menuName = menuName.trim();
        menu.category = category;
        menu.description = description.trim();
        menu.servings = servings;
        menu.cookTime = cookTime;
        menu.cookMinute = cookMinute;
        menu.ingredients = ingredients.trim();
        menu.steps = steps.trim();
        menu.difficultyLvl = difficultyLvl;
        menu.createdId = adminId;

        return menu;
    }

    // 기존 메뉴 정보 수정 (최초 등록 정보는 유지하고 수정 관리자와 수정 시각 기록)
    public void update(
            String menuName,
            String category,
            String description,
            Integer servings,
            Integer cookTime,
            Integer cookMinute,
            String ingredients,
            String steps,
            String difficultyLvl,
            String adminId
    ) {
        this.menuName = menuName.trim();
        this.category = category;
        this.description = description.trim();
        this.servings = servings;
        this.cookTime = cookTime;
        this.cookMinute = cookMinute;
        this.ingredients = ingredients.trim();
        this.steps = steps.trim();
        this.difficultyLvl = difficultyLvl;
        this.updatedId = adminId;
        this.updatedDt = LocalDateTime.now();
    }
}