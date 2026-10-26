package com.zipbob.zipbob_backend.service;

import com.zipbob.zipbob_backend.admin.common.AdminPagination;
import com.zipbob.zipbob_backend.dto.MenuListResponseDto;
import com.zipbob.zipbob_backend.dto.MenuResponseDto;
import com.zipbob.zipbob_backend.entity.Menu;
import com.zipbob.zipbob_backend.repository.MenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;


import java.util.List;
// Spring이 관리하는 서비스 객체로 등록
@Service
// Repository를 받는 생성자 생성
@RequiredArgsConstructor
// 메서드를 읽기 중심의 트랜잭션으로 실행, 조회 작업 의도 (readOnly = true)
@Transactional(readOnly = true)
public class MenuService {

    private final MenuRepository menuRepository;

    // 메뉴 레시피 조회 (메뉴 정보·이미지·인분·시간·난이도·재료·조리 순서)
    public MenuResponseDto getMenu(Integer menuId) {
        if (menuId == null || menuId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "메뉴 ID는 양수여야 합니다."
            );
        }

        Menu menu = menuRepository.findVisibleByIdWithFiles(menuId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "메뉴를 찾을 수 없거나 현재 공개되지 않은 메뉴입니다."
                ));

        return new MenuResponseDto(menu);
    }

    // 전체 메뉴 (ID·메뉴명·카테고리·설명·이미지)
    public List<MenuListResponseDto> getMenus(
            String keyword,
            String category
    ) {
        // 값이 없으면 빈 문자열, 값이 있으면 앞뒤 공백 제거
        String searchKeyword = keyword == null ? "" : keyword.trim();
        String searchCategory = category == null ? "" : category.trim();

        return menuRepository.searchMenus(searchKeyword, searchCategory)
                .stream()
                // DTO 목록으로 변환
                .map(MenuListResponseDto::new)
                // List 형태로 변환
                .toList();
    }

    // 관리자 메뉴 목록: 한 페이지당 20개 조회
    public Page<MenuListResponseDto> getMenusPage(
            String keyword,
            String category,
            int page
    ) {
        String searchKeyword = keyword == null ? "" : keyword.trim();
        String searchCategory = category == null ? "" : category.trim();

        // 화면에서는 1페이지부터 사용
        int currentPage = Math.max(page, 1);

        // Spring Data JPA의 페이지 번호는 0부터 시작
        PageRequest pageRequest =
                AdminPagination.pageRequest(currentPage);

        return menuRepository
                .searchMenusPage(searchKeyword, searchCategory, pageRequest)
                .map(MenuListResponseDto::new);
    }
}