package com.zipbob.zipbob_backend.service;

import com.zipbob.zipbob_backend.dto.MenuListResponseDto;
import com.zipbob.zipbob_backend.dto.MenuResponseDto;
import com.zipbob.zipbob_backend.entity.Menu;
import com.zipbob.zipbob_backend.repository.MenuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

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
        Menu menu = menuRepository.findByIdWithFiles(menuId)
                // 메뉴가 없으면
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "메뉴를 찾을 수 없습니다."
                ));
        // 메뉴가 있으면
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
}