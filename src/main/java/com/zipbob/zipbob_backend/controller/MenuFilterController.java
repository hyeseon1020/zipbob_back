package com.zipbob.zipbob_backend.controller;

import com.zipbob.zipbob_backend.dto.MenuFilterResponseDto;
import com.zipbob.zipbob_backend.service.MenuFilterService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/menufilter")
@RequiredArgsConstructor
public class MenuFilterController {
    private final MenuFilterService menuFilterService;
    /**
     * 태그 및 카테고리명이 포함된 필터링 메뉴 목록 조회 API
     * GET http://localhost:8080/api/menufilter/filter
     */
    @GetMapping
    public ResponseEntity<List<MenuFilterResponseDto>> getFilteredMenuList() {
        return ResponseEntity.ok(menuFilterService.getMenuListWithTags());
    }

}
