package com.zipbob.zipbob_backend.repository;

import com.zipbob.zipbob_backend.dto.LikeListProjection;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("integration")
@AutoConfigureTestDatabase(
        replace = AutoConfigureTestDatabase.Replace.NONE
)
class LikeRepositoryIntegrationTest {

    private static final String USER_A = "it_like_user_a";
    private static final String USER_B = "it_like_user_b";
    private static final String CATEGORY = "it_like_category";

    @Autowired
    private LikeRepository likeRepository;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    private Integer menuA;
    private Integer menuB;

    @BeforeEach
    void setUp() {
        // 외래키 조건을 만족하는 테스트 회원을 먼저 생성
        insertUser(USER_A, "즐겨찾기테스트A");
        insertUser(USER_B, "즐겨찾기테스트B");

        // 조회 쿼리의 활성 카테고리 조건을 만족하도록 생성
        jdbcTemplate.update("""
                INSERT INTO zipbob_app.code_mng (
                    code_id,
                    code_name,
                    code_parent,
                    code_depth,
                    use_yn,
                    created_id
                )
                VALUES (?, ?, ?, ?, ?, ?)
                """,
                CATEGORY,
                "테스트카테고리",
                "category_code",
                2,
                "Y",
                USER_A
        );

        menuA = insertMenu("통합테스트 메뉴A");
        menuB = insertMenu("통합테스트 메뉴B");
    }

    @Test
    @DisplayName("같은 회원이 같은 메뉴를 중복 등록해도 한 건만 저장된다")
    void insertIfAbsent_preventsDuplicateFavorite() {
        int firstInsert = likeRepository.insertIfAbsent(USER_A, menuA);
        int secondInsert = likeRepository.insertIfAbsent(USER_A, menuA);

        assertThat(firstInsert).isEqualTo(1);
        assertThat(secondInsert).isZero();

        assertThat(countFavorite(USER_A, menuA)).isEqualTo(1L);
    }

    @Test
    @DisplayName("즐겨찾기 목록에는 해당 회원이 등록한 메뉴만 포함된다")
    void findFavoriteMenus_returnsOnlyRequestedUsersFavorites() {
        likeRepository.insertIfAbsent(USER_A, menuA);
        likeRepository.insertIfAbsent(USER_B, menuB);

        List<LikeListProjection> userAFavorites =
                likeRepository.findFavoriteMenus(USER_A);

        List<LikeListProjection> userBFavorites =
                likeRepository.findFavoriteMenus(USER_B);

        assertThat(userAFavorites)
                .extracting(LikeListProjection::getMenuId)
                .containsExactly(menuA);

        assertThat(userBFavorites)
                .extracting(LikeListProjection::getMenuId)
                .containsExactly(menuB);

        // PostgreSQL 함수 결과와 기본 작성일도 확인
        LikeListProjection favorite = userAFavorites.getFirst();

        assertThat(favorite.getMenuName()).isEqualTo("통합테스트 메뉴A");
        assertThat(favorite.getCategoryName()).isEqualTo("테스트카테고리");
        assertThat(favorite.getCreatedDt()).isNotNull();
    }

    @Test
    @DisplayName("즐겨찾기를 삭제해도 다른 회원의 동일 메뉴는 유지된다")
    void deleteFavorite_preservesOtherUsersFavorite() {
        likeRepository.insertIfAbsent(USER_A, menuA);
        likeRepository.insertIfAbsent(USER_B, menuA);

        int deleted = likeRepository.deleteFavorite(USER_A, menuA);

        assertThat(deleted).isEqualTo(1);
        assertThat(countFavorite(USER_A, menuA)).isZero();
        assertThat(countFavorite(USER_B, menuA)).isEqualTo(1L);

        assertThat(likeRepository.findFavoriteMenus(USER_A)).isEmpty();

        assertThat(likeRepository.findFavoriteMenus(USER_B))
                .extracting(LikeListProjection::getMenuId)
                .containsExactly(menuA);
    }

    @Test
    @DisplayName("카테고리가 비활성화되면 즐겨찾기 목록에서 제외된다")
    void findFavoriteMenus_excludesInactiveCategory() {
        likeRepository.insertIfAbsent(USER_A, menuA);

        // 활성 상태에서는 조회되는지 먼저 확인
        assertThat(likeRepository.findFavoriteMenus(USER_A))
                .extracting(LikeListProjection::getMenuId)
                .containsExactly(menuA);

        jdbcTemplate.update("""
                UPDATE zipbob_app.code_mng
                SET use_yn = 'N'
                WHERE code_id = ?
                """,
                CATEGORY
        );

        assertThat(likeRepository.findFavoriteMenus(USER_A)).isEmpty();

        // 조회에서만 제외되고, 즐겨찾기 데이터는 유지
        assertThat(countFavorite(USER_A, menuA)).isEqualTo(1L);
    }

    private void insertUser(String userId, String userName) {
        jdbcTemplate.update("""
                INSERT INTO zipbob_app.user_mng (
                    user_id,
                    user_pw,
                    user_name,
                    user_role,
                    created_id
                )
                VALUES (?, ?, ?, ?, ?)
                """,
                userId,
                "unused-test-password",
                userName,
                "USER",
                userId
        );
    }

    private Integer insertMenu(String menuName) {
        // DB 시퀀스로 ID를 생성하고 실제 생성된 값을 반환
        return jdbcTemplate.queryForObject("""
                INSERT INTO zipbob_app.menu_mng (
                    menu_name,
                    category,
                    description,
                    created_id
                )
                VALUES (?, ?, ?, ?)
                RETURNING menu_id
                """,
                Integer.class,
                menuName,
                CATEGORY,
                "즐겨찾기 통합 테스트용 메뉴",
                USER_A
        );
    }

    private long countFavorite(String userId, Integer menuId) {
        Long count = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM zipbob_app.like_mng
                WHERE user_id = ?
                  AND menu_id = ?
                """,
                Long.class,
                userId,
                menuId
        );

        return count == null ? 0L : count;
    }
}