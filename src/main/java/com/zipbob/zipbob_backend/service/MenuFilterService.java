package com.zipbob.zipbob_backend.service;

import com.zipbob.zipbob_backend.dto.MenuFilterResponseDto;
import com.zipbob.zipbob_backend.dto.MenuListProjection;
import com.zipbob.zipbob_backend.dto.MenuTagListProjection;
import com.zipbob.zipbob_backend.repository.MenuFilterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j //로그 체크
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MenuFilterService {

    private final MenuFilterRepository menuFilterRepository;

    /**
     * 1. 태그 목록이 포함된 전체 메뉴 조회 (그룹화 로직 포함)
     */
    public List<MenuFilterResponseDto> getMenuListWithTags() {
        List<MenuTagListProjection> projections = menuFilterRepository.findAllWithCategoryName();

        // 순서를 보장하면서 menuId 기준으로 그룹화
        Map<Integer, MenuFilterResponseDto> menuMap = new LinkedHashMap<>();

        for (MenuTagListProjection p : projections) {
            // Map에 해당 menuId가 없으면 생성하여 추가
            MenuFilterResponseDto dto = menuMap.computeIfAbsent(
                    p.getMenuId(),
                    id -> new MenuFilterResponseDto(p)
            );

            // 태그 정보가 존재하는 경우에만 tags 리스트에 추가 (LEFT JOIN 특성 대응)
            if (p.getTagSeq() != null) {
                dto.getTags().add(new MenuFilterResponseDto.TagDto(p));
            }
        }

        return new ArrayList<>(menuMap.values());
    }

    /**
     * 프론트에서 받은 카테고리 및 태그 조건으로 필터링된 메뉴 조회
     */
    public List<MenuFilterResponseDto> getRecommendedMenus(String category, List<String> tags) {
        // 태그 리스트가 비어있는지 여부 체크
        boolean hasTags = (tags != null && !tags.isEmpty());

        // 태그가 비어있을 경우 SQL IN 절 에러를 방지하기 위해 빈 임시 리스트 전달 가능성 대비
        List<String> queryTags = hasTags ? tags : List.of("");

        // 리포지토리 호출
        List<MenuListProjection> results = menuFilterRepository.findFilteredMenus(category, queryTags, hasTags);

        // DTO로 변환
        List<MenuFilterResponseDto> responseList = results.stream()
                .map(MenuFilterResponseDto::new)
                .toList();

        // 💡 results 대신 변환된 responseList를 로그
        log.info("ttttttttttt: {}", responseList);

        return responseList;
    }
}