package com.zipbob.zipbob_backend.controller;

import com.zipbob.zipbob_backend.dto.LikeResponseDto;
import com.zipbob.zipbob_backend.service.LikeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
// PUT /api/favorites/10?userDeviceId=1
@RequestMapping("/api/favorites")
@RequiredArgsConstructor
public class LikeController {

    private final LikeService likeService;

    // 즐겨찾기 등록
    @PutMapping("/{menuId}")
    public ResponseEntity<Void> addFavorite(
            @PathVariable("menuId") Integer menuId,
            @RequestParam("userDeviceId") Integer userDeviceId
    ) {
        likeService.addFavorite(userDeviceId, menuId);

        return ResponseEntity.noContent().build();
    }

    // 즐겨찾기 삭제
    @DeleteMapping("/{menuId}")
    public ResponseEntity<Void> removeFavorite(
            @PathVariable("menuId") Integer menuId,
            @RequestParam("userDeviceId") Integer userDeviceId
    ) {
        likeService.removeFavorite(userDeviceId, menuId);

        return ResponseEntity.noContent().build();
    }

    // 기기별 즐겨찾기 목록 조회
    @GetMapping
    public ResponseEntity<List<LikeResponseDto>> getFavorites(
            @RequestParam("userDeviceId") Integer userDeviceId
    ) {
        return ResponseEntity.ok(
                likeService.getFavorites(userDeviceId)
        );
    }
}