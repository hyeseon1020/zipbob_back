package com.zipbob.zipbob_backend.service;

import com.zipbob.zipbob_backend.dto.LikeResponseDto;
import com.zipbob.zipbob_backend.repository.LikeRepository;
import com.zipbob.zipbob_backend.repository.MenuRepository;
import com.zipbob.zipbob_backend.repository.UserDeviceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
// 조회용 트랜잭션으로 설정
@Transactional(readOnly = true)
public class LikeService {

    private final LikeRepository likeRepository;
    private final UserDeviceRepository userDeviceRepository;
    private final MenuRepository menuRepository;

    // 즐겨찾기 등록
    @Transactional
    public void addFavorite(Integer userDeviceId, Integer menuId) {
        validateDevice(userDeviceId);
        validateMenu(menuId);
        // 즐겨찾기 등록 — 이미 등록돼 있으면 유지
        likeRepository.insertIfAbsent(userDeviceId, menuId);
    }

    // 즐겨찾기 삭제
    @Transactional
    public void removeFavorite(Integer userDeviceId, Integer menuId) {
        validateDevice(userDeviceId);
        validatePositiveId(menuId, "메뉴 ID");

        likeRepository.deleteFavorite(userDeviceId, menuId);
    }

    // 즐겨찾기 목록 조회
    public List<LikeResponseDto> getFavorites(Integer userDeviceId) {
        validateDevice(userDeviceId);

        return likeRepository.findFavoriteMenus(userDeviceId)
                .stream()
                .map(LikeResponseDto::new)
                .toList();
    }

    // 서버에 등록된 기기인지 확인
    private void validateDevice(Integer userDeviceId) {
        validatePositiveId(userDeviceId, "기기 ID");

        if (!userDeviceRepository.existsById(userDeviceId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "등록되지 않은 기기입니다."
            );
        }
    }

    // 실제 존재하는 메뉴인지 확인
    private void validateMenu(Integer menuId) {
        validatePositiveId(menuId, "메뉴 ID");

        if (!menuRepository.existsById(menuId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "존재하지 않는 메뉴입니다."
            );
        }
    }

    // 검사 : 기기 ID나 메뉴 ID가 정상적인 양수인지 확인 (0 : 오류 / 1 : 정상)
    private void validatePositiveId(Integer id, String name) {
        if (id == null || id <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    name + "는 양수여야 합니다."
            );
        }
    }
}