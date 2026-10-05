package com.zipbob.zipbob_backend.controller;

import com.zipbob.zipbob_backend.dto.MenuResponseDto;
import com.zipbob.zipbob_backend.service.MenuService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/menus")
@RequiredArgsConstructor
public class MenuController {

    private final MenuService menuService;

    @GetMapping("/{menuId}")
    public MenuResponseDto getMenu(
            @PathVariable("menuId") Integer menuId
    ) {
        return menuService.getMenu(menuId);
    }
}