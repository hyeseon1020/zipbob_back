package com.zipbob.zipbob_backend.repository;

import com.zipbob.zipbob_backend.dto.RecentRecipeListProjection;
import com.zipbob.zipbob_backend.entity.RecentRecipe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface RecentRecipeRepository extends JpaRepository<RecentRecipe, Integer> {
    /*
    1. 해당 기기에서 이미 본 메뉴인지 조회
    결과 있음 → Service에서 updateViewedDt() 호출
    결과 없음 → Service에서 Entity 생성 후 save() 호출
    */
    Optional<RecentRecipe> findByUserDeviceIdAndMenuId(
            Integer userDeviceId,
            Integer menuId
    );

    /* 2. 최대 5개 유지 처리에 사용할 기록 전체 조회
    최신 기록부터 정렬하고, 시간이 같으면 기록 ID가 큰 것을 먼저 정렬
    */
    List<RecentRecipe>
    findAllByUserDeviceIdOrderByViewedDtDescRecentRecipeIdDesc(
            Integer userDeviceId
    );

    // 3. 홈 화면에 표시할 최근 본 레시피 목록 조회
    @Query(value = """
        SELECT
            m.menu_id AS "menuId",
            m.menu_name AS "menuName",
            m.category AS "category",
            -- 카테고리 코드를 공통코드의 한글 이름으로 변환
            zipbob_app.codename_f(m.category) AS "categoryName",
            m.description AS "description",
            (
                -- 대표 이미지: 사용 중인 파일 중 ID가 가장 작은 파일 1개
                SELECT f.file_path
                FROM zipbob_app.file_mng f
                WHERE f.menu_id = m.menu_id
                  AND f.use_yn = 'Y'
                ORDER BY f.file_id ASC
                LIMIT 1
            ) AS "imageUrl",
            -- 마지막으로 레시피를 본 시간
            r.viewed_dt AS "viewedDt"
        FROM zipbob_app.recent_recipe_mng r
        -- 최근 본 기록에 해당 메뉴의 정보를 연결
        INNER JOIN zipbob_app.menu_mng m
            ON m.menu_id = r.menu_id
        -- 전달받은 기기의 최근 본 기록만 조회
        WHERE r.user_device_id = :userDeviceId
        -- 최신 조회순으로 정렬, 시간이 같으면 기록 ID가 큰 순서
        ORDER BY
            r.viewed_dt DESC,
            r.recent_recipe_id DESC
        -- 최근 본 레시피를 최대 5개 반환
        LIMIT 5
        """, nativeQuery = true)
    List<RecentRecipeListProjection> findRecentRecipeMenus(
            @Param("userDeviceId") Integer userDeviceId
    );
}