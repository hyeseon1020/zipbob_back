package com.zipbob.zipbob_backend.controller;

import com.zipbob.zipbob_backend.dto.RecentRecipeResponseDto;
import com.zipbob.zipbob_backend.service.RecentRecipeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/recent-recipes")
@RequiredArgsConstructor
public class RecentRecipeController {

    private final RecentRecipeService recentRecipeService;

    // 최근 본 기록 저장 또는 마지막 조회 시간 갱신 (PUT /api/recent-recipes/1)
    @PutMapping("/{menuId}")
    public ResponseEntity<Void> recordView(
            @PathVariable("menuId") Integer menuId,
            Authentication authentication
    ) {
        recentRecipeService.recordView(
                authentication.getName(),
                menuId
        );

        return ResponseEntity.noContent().build();
    }

    // 최근 본 레시피 목록 조회
    @GetMapping
    public ResponseEntity<List<RecentRecipeResponseDto>> getRecentRecipes(
            Authentication authentication
    ) {
        return ResponseEntity.ok()
                .header("Cache-Control", "no-store")
                .body(
                        recentRecipeService.getRecentRecipes(
                                authentication.getName()
                        )
                );
    }
}