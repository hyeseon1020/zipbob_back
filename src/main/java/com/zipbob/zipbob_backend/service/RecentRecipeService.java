package com.zipbob.zipbob_backend.service;

import com.zipbob.zipbob_backend.dto.RecentRecipeResponseDto;
import com.zipbob.zipbob_backend.entity.RecentRecipe;
import com.zipbob.zipbob_backend.repository.MenuRepository;
import com.zipbob.zipbob_backend.repository.RecentRecipeRepository;
import com.zipbob.zipbob_backend.repository.UserDeviceRepository;
import com.zipbob.zipbob_backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class RecentRecipeService {

    // 기기별로 유지할 최근 본 레시피 개수
    private static final int MAX_RECENT_RECIPES = 5;

    private final RecentRecipeRepository recentRecipeRepository;
    private final UserRepository userRepository;
    private final MenuRepository menuRepository;

    // 최근 본 레시피 기록 저장 또는 조회 시간 갱신
    @Transactional
    public void recordView(String  userId, Integer menuId) {

        // 1. 전달받은 ID 검증
        validateUserId(userId);
        validateMenuId(menuId);

        // 2. 여러 기기에서 들어오는 같은 회원의 요청을 직렬화
        userRepository.findByIdForUpdate(userId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND,
                        "회원 정보를 찾을 수 없습니다."
                ));

        // 3. 메뉴 존재 확인
        if (!menuRepository.existsById(menuId)) {
            throw new ResponseStatusException(
                    HttpStatus.NOT_FOUND,
                    "메뉴를 찾을 수 없습니다."
            );
        }

        // 4. 기존 기록 조회
        Optional<RecentRecipe> existingRecord =
                recentRecipeRepository.findByUserIdAndMenuId(
                        userId,
                        menuId
                );

        // 잠금을 획득한 후 현재 시간을 생성
        LocalDateTime viewedDt = LocalDateTime.now();

        if (existingRecord.isPresent()) {

            // 5-1. 이미 본 메뉴: 기존 Entity의 조회 시간만 변경
            // JPA 변경 감지를 사용하므로 별도 save() 호출이 필요 없음
            existingRecord.get().updateViewedDt(viewedDt);

        } else {

            // 5-2. 처음 본 메뉴: Entity 생성 후 저장
            RecentRecipe recentRecipe = new RecentRecipe(
                    userId,
                    menuId,
                    viewedDt
            );

            recentRecipeRepository.save(recentRecipe);
        }

        // 6. 저장·변경 내용을 DB에 반영한 후 최신순으로 조회
        // flush는 커밋이 아니므로 이후 실패하면 함께 롤백됨
        recentRecipeRepository.flush();

        List<RecentRecipe> records =
                recentRecipeRepository
                        .findAllByUserIdOrderByViewedDtDescRecentRecipeIdDesc(
                                userId
                        );

        // 7. 앞의 5개를 유지하고, 6번째부터 삭제
        if (records.size() > MAX_RECENT_RECIPES) {

            List<RecentRecipe> recordsToDelete =
                    records.subList(MAX_RECENT_RECIPES, records.size());

            recentRecipeRepository.deleteAll(recordsToDelete);
        }
    }

    // 유저별 최근 본 레시피 목록 조회
    @Transactional(readOnly = true)
    public List<RecentRecipeResponseDto> getRecentRecipes(String  userId) {

        validateUserId(userId);

        // 존재하지 않는 유저와 기록이 없는 유저를 구분
        if (!userRepository.existsById(userId)) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "회원 정보를 찾을 수 없습니다."
            );
        }

        // Projection을 API 응답 DTO로 변환
        return recentRecipeRepository
                .findRecentRecipeMenus(userId)
                .stream()
                .map(RecentRecipeResponseDto::new)
                .toList();
    }

    private void validateUserId(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new ResponseStatusException(
                    HttpStatus.UNAUTHORIZED,
                    "로그인이 필요합니다."
            );
        }
    }

    private void validateMenuId(Integer menuId) {
        if (menuId == null || menuId <= 0) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "메뉴 ID는 양수여야 합니다."
            );
        }
    }
}