package com.zipbob.zipbob_backend.controller;

import com.zipbob.zipbob_backend.dto.RecentRecipeResponseDto;
import com.zipbob.zipbob_backend.service.RecentRecipeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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

    // 최근 본 기록 저장 또는 마지막 조회 시간 갱신 (PUT /api/recent-recipes/3?userDeviceId=10)
    @PutMapping("/{menuId}")
    public ResponseEntity<Void> recordView(
            @PathVariable("menuId") Integer menuId,
            @RequestParam("userDeviceId") Integer userDeviceId
    ) {

        recentRecipeService.recordView(userDeviceId, menuId);

        // 처리 성공: 응답 본문 없이 204 반환
        return ResponseEntity.noContent().build();
    }

    // 기기별 최근 본 레시피 목록 조회 (GET /api/recent-recipes?userDeviceId=10)
    @GetMapping
    public ResponseEntity<List<RecentRecipeResponseDto>> getRecentRecipes(
            @RequestParam("userDeviceId") Integer userDeviceId
    ) {

        List<RecentRecipeResponseDto> recipes =
                recentRecipeService.getRecentRecipes(userDeviceId);

        // 처리 성공: 목록을 JSON으로 반환
        return ResponseEntity.ok(recipes);
    }
}