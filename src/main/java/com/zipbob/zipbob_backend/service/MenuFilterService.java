package com.zipbob.zipbob_backend.service;

import com.zipbob.zipbob_backend.dto.MenuFilterResponseDto;
import com.zipbob.zipbob_backend.dto.MenuTagListProjection;
import com.zipbob.zipbob_backend.repository.MenuFilterRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MenuFilterService {

    private final MenuFilterRepository menuFilterRepository;

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
}