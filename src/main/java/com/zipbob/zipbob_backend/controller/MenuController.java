package com.zipbob.zipbob_backend.controller;

import com.zipbob.zipbob_backend.dto.MenuListResponseDto;
import com.zipbob.zipbob_backend.dto.MenuResponseDto;
import com.zipbob.zipbob_backend.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/menus")
// Lombok이 final 필드를 받는 생성자를 생성
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    // 메뉴 레시피 조회 (메뉴 정보·이미지·인분·시간·난이도·재료·조리 순서)
    @GetMapping("/{menuId}")
    public MenuResponseDto getMenu(
            @PathVariable("menuId") Integer menuId
    ) {
        return menuService.getMenu(menuId);
    }

    // 전체 메뉴 (ID·메뉴명·카테고리·설명·이미지)
    // 따로 주소가 없는 경우 (공통 주소인 /api/menus를 사용)
    @GetMapping
    public List<MenuListResponseDto> getMenus(
            // GET /api/menus?keyword=계란&category=rice_bowl : ? 뒤에 있는 검 색 조건을 읽는다
            // menuService.getMenus("계란", "rice_bowl");
            @RequestParam(name = "keyword", defaultValue = "") String keyword,
            @RequestParam(name = "category", defaultValue = "") String category
    ){
        return menuService.getMenus(keyword, category);
    }

}