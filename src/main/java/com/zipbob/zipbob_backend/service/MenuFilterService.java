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
        // 선택한 특징에서 null, 빈 값, 중복 제거
        List<String> selectedTags = tags == null
                ? List.of()
                : tags.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(tag -> !tag.isEmpty())
                .distinct()
                .toList();

        boolean hasTags = !selectedTags.isEmpty();

        // 특징 미선택 시 빈 IN 목록 방지
        List<String> queryTags = hasTags ? selectedTags : List.of("");

        // 선택한 특징 개수까지 전달
        List<MenuListProjection> results =
                menuFilterRepository.findFilteredMenus(
                        category,
                        queryTags,
                        hasTags,
                        selectedTags.size()
                );

        // DTO로 변환
        List<MenuFilterResponseDto> responseList = results.stream()
                .map(MenuFilterResponseDto::new)
                .toList();

        // 💡 results 대신 변환된 responseList를 로그
        log.debug(
                "조건 추천 조회: category={}, tags={}, resultCount={}",
                category,
                selectedTags,
                responseList.size()
        );

        return responseList;
    }
}