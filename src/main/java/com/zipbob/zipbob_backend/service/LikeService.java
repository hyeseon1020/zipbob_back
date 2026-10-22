package com.zipbob.zipbob_backend.service;

import com.zipbob.zipbob_backend.dto.LikeResponseDto;
import com.zipbob.zipbob_backend.repository.LikeRepository;
import com.zipbob.zipbob_backend.repository.MenuRepository;
import com.zipbob.zipbob_backend.repository.UserRepository;
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
    private final UserRepository userRepository;
    private final MenuRepository menuRepository;

    // 즐겨찾기 등록
    @Transactional
    public void addFavorite(String userId, Integer menuId) {
        validateUser(userId);
        validateMenu(menuId);
        // 즐겨찾기 등록 — 이미 등록돼 있으면 유지
        likeRepository.insertIfAbsent(userId, menuId);
    }

    // 즐겨찾기 삭제
    @Transactional
    public void removeFavorite(String userId, Integer menuId) {
        validateUser(userId);
        validatePositiveId(menuId);

        likeRepository.deleteFavorite(userId, menuId);
    }

    // 즐겨찾기 목록 조회
    public List<LikeResponseDto> getFavorites(String  userId) {
        validateUser(userId);

        return likeRepository.findFavoriteMenus(userId)
                .stream()
                .map(LikeResponseDto::new)
                .toList();
    }

    // 서버에 등록된 기기인지 확인
    private void validateUser(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "로그인이 필요합니다."
            );
        }

        if (!userRepository.existsById(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "회원 정보를 찾을 수 없습니다."
            );
        }
    }

    // 실제 존재하는 메뉴인지 확인
    private void validateMenu(Integer menuId) {
        validatePositiveId(menuId);

        if (!menuRepository.existsById(menuId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "존재하지 않는 메뉴입니다."
            );
        }
    }

    // 검사 : 기기 ID나 메뉴 ID가 정상적인 양수인지 확인 (0 : 오류 / 1 : 정상)
    private void validatePositiveId(Integer id) {
        if (id == null || id <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "메뉴 ID는 양수여야 합니다."
            );
        }
    }
}