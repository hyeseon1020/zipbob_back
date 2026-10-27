package com.zipbob.zipbob_backend.admin.service;

import com.zipbob.zipbob_backend.admin.repository.TagRepository;
import com.zipbob.zipbob_backend.entity.Menu;
import com.zipbob.zipbob_backend.entity.Tag;
import com.zipbob.zipbob_backend.repository.CodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminMenuTagService {

    private final TagRepository tagRepository;
    private final CodeRepository codeRepository;

    // 수정 팝업에서 사용할 기존 태그 코드 조회
    public List<String> getTagIds(Integer menuId) {
        return tagRepository.findActiveCodeIdsByMenuId(menuId);
    }

    // 신규 메뉴의 태그 연결 등록
    @Transactional
    public void create(
            Menu menu,
            List<String> requestedTagIds,
            String adminId
    ) {
        Set<String> selectedTagIds = validateTagIds(requestedTagIds);

        saveTags(menu, selectedTagIds, adminId);
    }

    // 기존 연결 삭제 후 현재 선택한 태그로 교체
    @Transactional
    public void replace(
            Menu menu,
            List<String> requestedTagIds,
            String adminId
    ) {
        // 삭제 전에 선택한 태그가 유효한지 먼저 검사
        Set<String> selectedTagIds = validateTagIds(requestedTagIds);

        tagRepository.deleteAllByMenuId(menu.getMenuId());

        saveTags(menu, selectedTagIds, adminId);
    }

    // 태그 유효성 검사 및 중복 제거
    private Set<String> validateTagIds(List<String> requestedTagIds) {
        if (requestedTagIds == null || requestedTagIds.isEmpty()) {
            throw new IllegalArgumentException(
                    "음식 특징을 최소 1개 선택해주세요."
            );
        }

        Set<String> availableTagIds = codeRepository
                .findByCodeParent("tag_code")
                .stream()
                .map(code -> code.getCodeId())
                .collect(Collectors.toSet());

        Set<String> selectedTagIds = new LinkedHashSet<>();

        for (String tagId : requestedTagIds) {
            if (tagId == null || !availableTagIds.contains(tagId)) {
                throw new IllegalArgumentException(
                        "사용할 수 없는 음식 특징이 포함되어 있습니다."
                );
            }

            selectedTagIds.add(tagId);
        }

        return selectedTagIds;
    }

    // 선택한 태그의 공통코드 사용 상태를 반영해서 저장
    private void saveTags(
            Menu menu,
            Set<String> selectedTagIds,
            String adminId
    ) {
        Map<String, String> tagUseYn = codeRepository
                .findByCodeParent("tag_code")
                .stream()
                .collect(Collectors.toMap(
                        code -> code.getCodeId(),
                        code -> code.getUseYn()
                ));

        List<Tag> tags = selectedTagIds.stream()
                .map(tagId -> Tag.create(
                        menu,
                        tagId,
                        tagUseYn.get(tagId),
                        adminId
                ))
                .toList();

        tagRepository.saveAll(tags);
    }
}