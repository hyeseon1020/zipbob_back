package com.zipbob.zipbob_backend.admin.controller;

import com.zipbob.zipbob_backend.admin.dto.MenuCreateRequestDto;
import com.zipbob.zipbob_backend.admin.service.AdminMenuService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/admin/menus")
@RequiredArgsConstructor
// 관리자 메뉴 데이터 조회·등록
public class AdminMenuController {

    private final AdminMenuService adminMenuService;

    // 등록 팝업에 표시할 카테고리·난이도 목록 반환
    @GetMapping("/form-options")
    public AdminMenuService.FormOptions formOptions() {
        return adminMenuService.getFormOptions();
    }

    // 입력한 메뉴 정보·사진을 받아 서비스에 등록 요청
    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> create(
            @Valid @ModelAttribute MenuCreateRequestDto request,
            BindingResult bindingResult,
            Principal principal
    ) {
        if (bindingResult.hasErrors()) {
            String message = bindingResult.getAllErrors()
                    .stream()
                    .map(error -> error.getDefaultMessage())
                    .collect(Collectors.joining("\n"));

            return ResponseEntity.badRequest()
                    .body(Map.of("message", message));
        }

        Integer menuId = adminMenuService.create(
                request,
                principal.getName()
        );

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of(
                        "menuId", menuId,
                        "message", "신규 메뉴가 등록되었습니다."
                ));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleBadRequest(
            IllegalArgumentException exception
    ) {
        return ResponseEntity.badRequest()
                .body(Map.of(
                        "message", exception.getMessage()
                ));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<?> handleSaveFailure(
            IllegalStateException exception
    ) {
        log.error("관리자 메뉴 등록 실패", exception);

        return ResponseEntity.internalServerError()
                .body(Map.of(
                        "message",
                        "메뉴 저장에 실패했습니다. 잠시 후 다시 시도해주세요."
                ));
    }
}