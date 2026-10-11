package com.zipbob.zipbob_backend.controller;

import com.zipbob.zipbob_backend.dto.MenuFilterResponseDto;
import com.zipbob.zipbob_backend.dto.MenuRecommendRequestDto;
import com.zipbob.zipbob_backend.service.MenuFilterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/menufilter")
@RequiredArgsConstructor
public class MenuFilterController {
    private final MenuFilterService menuFilterService;
    /**
     * 태그 및 카테고리명이 포함된 필터링 메뉴 목록 조회 API
     * GET http://localhost:8080/api/menufilter
     */
    @GetMapping
    public ResponseEntity<List<MenuFilterResponseDto>> getFilteredMenuList() {
        return ResponseEntity.ok(menuFilterService.getMenuListWithTags());
    }

    /**
     * RecommendService();
     * 프론트에서 보낸 카테고리 및 태그를 받아 메뉴 추천/필터링 결과를 반환하는 API
     * POST http://localhost:8080/api/menufilter/recommend
     */
    @PostMapping("/recommend")
    public ResponseEntity<List<MenuFilterResponseDto>> recommend(@RequestBody MenuRecommendRequestDto request){
        String category = request.getCategory();
        List<String> tags = request.getTags();

        // 서비스에서 카테고리와 태그 조건으로 필터링된 메뉴 목록 조회
        List<MenuFilterResponseDto> recommendedMenus = menuFilterService.getRecommendedMenus(category, tags);

        return ResponseEntity.ok(recommendedMenus);
    }
}
