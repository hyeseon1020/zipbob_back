package com.zipbob.zipbob_backend.admin.controller;

import com.zipbob.zipbob_backend.admin.dto.MenuCreateRequestDto;
import com.zipbob.zipbob_backend.admin.service.AdminMenuService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.security.Principal;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@Slf4j
@RestController
@RequestMapping("/api/admin/menus")
@RequiredArgsConstructor
public class AdminMenuController {

    private final AdminMenuService adminMenuService;

    // 등록·수정 팝업의 카테고리·난이도·음식 특징 조회
    @GetMapping("/form-options")
    public AdminMenuService.FormOptions formOptions() {
        return adminMenuService.getFormOptions();
    }

    // 신규 메뉴 등록
    @PostMapping(
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
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
                    .body(Map.of(
                            "message", message
                    ));
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

    // 수정 팝업에 표시할 기존 메뉴 상세 조회
    @GetMapping("/{menuId}")
    public ResponseEntity<AdminMenuService.MenuEditDetail> detail(
            @PathVariable("menuId") Integer menuId
    ) {
        AdminMenuService.MenuEditDetail detail =
                adminMenuService.getDetail(menuId);

        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(detail);
    }

    // 기존 메뉴 수정
    @PutMapping(
            value = "/{menuId}",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<?> update(
            @PathVariable("menuId") Integer menuId,
            @Valid @ModelAttribute MenuCreateRequestDto request,
            BindingResult bindingResult,
            Principal principal
    ) {
        // 입력값 검증 실패 시 서비스 호출 중단
        if (bindingResult.hasErrors()) {
            String message = bindingResult.getAllErrors()
                    .stream()
                    .map(error -> error.getDefaultMessage())
                    .collect(Collectors.joining("\n"));

            return ResponseEntity.badRequest()
                    .body(Map.of(
                            "message", message
                    ));
        }

        adminMenuService.update(
                menuId,
                request,
                principal.getName()
        );

        return ResponseEntity.ok()
                .body(Map.of(
                        "menuId", menuId,
                        "message", "메뉴가 수정되었습니다."
                ));
    }

    // 조회·수정할 메뉴가 없는 경우
    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<?> handleNotFound(
            NoSuchElementException exception
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Map.of(
                        "message", exception.getMessage()
                ));
    }

    // 잘못된 코드·이미지 등 서비스 검증 실패
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleBadRequest(
            IllegalArgumentException exception
    ) {
        return ResponseEntity.badRequest()
                .body(Map.of(
                        "message", exception.getMessage()
                ));
    }

    // 파일 저장 등 서비스 처리 실패
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<?> handleSaveFailure(
            IllegalStateException exception
    ) {
        log.error("관리자 메뉴 저장 실패", exception);

        return ResponseEntity.internalServerError()
                .body(Map.of(
                        "message",
                        "메뉴 저장에 실패했습니다. 잠시 후 다시 시도해주세요."
                ));
    }
}