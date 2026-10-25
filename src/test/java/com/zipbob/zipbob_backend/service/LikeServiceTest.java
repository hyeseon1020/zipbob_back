package com.zipbob.zipbob_backend.service;

import com.zipbob.zipbob_backend.dto.LikeResponseDto;
import com.zipbob.zipbob_backend.repository.LikeRepository;
import com.zipbob.zipbob_backend.repository.MenuRepository;
import com.zipbob.zipbob_backend.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

// 회원·메뉴 검증과 즐겨찾기 저장·삭제·조회 요청을 확인하는 테스트
// DB 역할을 대신하는 Mock 객체를 사용
class LikeServiceTest {

    private LikeRepository likeRepository;
    private UserRepository userRepository;
    private MenuRepository menuRepository;
    private LikeService likeService;

    @BeforeEach
    void setUp() {
        likeRepository = mock(LikeRepository.class);
        userRepository = mock(UserRepository.class);
        menuRepository = mock(MenuRepository.class);

        likeService = new LikeService(
                likeRepository,
                userRepository,
                menuRepository
        );
    }

    @Test
    @DisplayName("존재하는 회원과 메뉴이면 해당 회원의 즐겨찾기를 등록한다")
    void addFavoriteWithValidUserAndMenu() {
        String userId = "test-user";
        Integer menuId = 1;

        when(userRepository.existsById(userId)).thenReturn(true);
        when(menuRepository.existsById(menuId)).thenReturn(true);

        likeService.addFavorite(userId, menuId);

        verify(likeRepository).insertIfAbsent(userId, menuId);
        verifyNoMoreInteractions(likeRepository);
    }

    @Test
    @DisplayName("회원 아이디가 없으면 등록을 거부한다")
    void rejectAddWithoutUserId() {
        for (String userId : new String[]{null, "", "   "}) {
            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> likeService.addFavorite(userId, 1)
            );

            assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        }

        verifyNoInteractions(
                userRepository,
                menuRepository,
                likeRepository
        );
    }

    @Test
    @DisplayName("존재하지 않는 회원이면 등록을 거부한다")
    void rejectAddForUnknownUser() {
        String userId = "unknown-user";

        when(userRepository.existsById(userId)).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> likeService.addFavorite(userId, 1)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        verifyNoInteractions(menuRepository, likeRepository);
    }

    @Test
    @DisplayName("메뉴 아이디가 null 또는 양수가 아니면 등록을 거부한다")
    void rejectAddWithInvalidMenuId() {
        String userId = "test-user";

        when(userRepository.existsById(userId)).thenReturn(true);

        for (Integer menuId : new Integer[]{null, 0, -1}) {
            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> likeService.addFavorite(userId, menuId)
            );

            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        }

        verifyNoInteractions(menuRepository, likeRepository);
    }

    @Test
    @DisplayName("존재하지 않는 메뉴이면 등록을 거부한다")
    void rejectAddForUnknownMenu() {
        String userId = "test-user";
        Integer menuId = 999;

        when(userRepository.existsById(userId)).thenReturn(true);
        when(menuRepository.existsById(menuId)).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> likeService.addFavorite(userId, menuId)
        );

        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
        verifyNoInteractions(likeRepository);
    }

    @Test
    @DisplayName("지정한 회원과 메뉴로 즐겨찾기 삭제를 요청한다")
    void removeFavoriteForSpecifiedUser() {
        String userId = "test-user";
        Integer menuId = 1;

        when(userRepository.existsById(userId)).thenReturn(true);

        likeService.removeFavorite(userId, menuId);

        verify(likeRepository).deleteFavorite(userId, menuId);
        verifyNoMoreInteractions(likeRepository);
    }

    @Test
    @DisplayName("회원 아이디가 없으면 삭제를 거부한다")
    void rejectRemoveWithoutUserId() {
        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> likeService.removeFavorite(null, 1)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        verifyNoInteractions(likeRepository);
    }

    @Test
    @DisplayName("잘못된 메뉴 아이디이면 삭제를 거부한다")
    void rejectRemoveWithInvalidMenuId() {
        String userId = "test-user";

        when(userRepository.existsById(userId)).thenReturn(true);

        for (Integer menuId : new Integer[]{null, 0, -1}) {
            ResponseStatusException exception = assertThrows(
                    ResponseStatusException.class,
                    () -> likeService.removeFavorite(userId, menuId)
            );

            assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        }

        verifyNoInteractions(likeRepository);
    }

    @Test
    @DisplayName("지정한 회원으로 조회하고 저장된 메뉴가 없으면 빈 목록을 반환한다")
    void getFavoritesReturnsEmptyList() {
        String userId = "test-user";

        when(userRepository.existsById(userId)).thenReturn(true);
        when(likeRepository.findFavoriteMenus(userId))
                .thenReturn(List.of());

        List<LikeResponseDto> result = likeService.getFavorites(userId);

        assertTrue(result.isEmpty());
        verify(likeRepository).findFavoriteMenus(userId);
        verifyNoMoreInteractions(likeRepository);
    }

    @Test
    @DisplayName("존재하지 않는 회원이면 목록 조회를 거부한다")
    void rejectGetFavoritesForUnknownUser() {
        String userId = "unknown-user";

        when(userRepository.existsById(userId)).thenReturn(false);

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> likeService.getFavorites(userId)
        );

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
        verifyNoInteractions(likeRepository);
    }
}