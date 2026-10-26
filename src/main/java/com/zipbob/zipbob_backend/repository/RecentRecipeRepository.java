package com.zipbob.zipbob_backend.repository;

import com.zipbob.zipbob_backend.dto.RecentRecipeListProjection;
import com.zipbob.zipbob_backend.entity.RecentRecipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RecentRecipeRepository
        extends JpaRepository<RecentRecipe, Integer> {

    // 해당 회원이 이미 조회한 메뉴인지 확인
    Optional<RecentRecipe> findByUserIdAndMenuId(
            String userId,
            Integer menuId
    );

    // 최신 5개 유지 처리에 사용할 기록 조회
    List<RecentRecipe>
    findAllByUserIdOrderByViewedDtDescRecentRecipeIdDesc(
            String userId
    );

    // 회원별 최근 조회 목록: 활성 카테고리 메뉴 중 최신 5개
    @Query(value = """
        SELECT
            m.menu_id AS "menuId",
            m.menu_name AS "menuName",
            m.category AS "category",
            zipbob_app.codename_f(m.category) AS "categoryName",
            m.description AS "description",
            (
                SELECT f.file_path
                FROM zipbob_app.file_mng f
                WHERE f.menu_id = m.menu_id
                  AND f.use_yn = 'Y'
                ORDER BY f.file_id ASC
                LIMIT 1
            ) AS "imageUrl",
            r.viewed_dt AS "viewedDt"
        FROM zipbob_app.recent_recipe_mng r
        INNER JOIN zipbob_app.menu_mng m
            ON m.menu_id = r.menu_id
        WHERE r.user_id = :userId
          AND EXISTS (
              SELECT 1
              FROM zipbob_app.code_mng c
              WHERE c.code_id = m.category
                AND c.code_parent = 'category_code'
                AND c.use_yn = 'Y'
          )
        ORDER BY
            r.viewed_dt DESC,
            r.recent_recipe_id DESC
        LIMIT 5
        """, nativeQuery = true)
    List<RecentRecipeListProjection> findRecentRecipeMenus(
            @Param("userId") String userId
    );
}