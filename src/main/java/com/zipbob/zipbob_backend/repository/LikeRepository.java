package com.zipbob.zipbob_backend.repository;

import com.zipbob.zipbob_backend.dto.LikeListProjection;
import com.zipbob.zipbob_backend.entity.Like;
import com.zipbob.zipbob_backend.entity.LikeId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LikeRepository extends JpaRepository<Like, LikeId> {

    // 기기별 즐겨찾기 메뉴 목록 조회
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
                l.crated_dt AS "createdDt"
            FROM zipbob_app.like_mng l
            -- 기기별 즐겨찾기를 찾고, 해당 메뉴의 정보를 연결            
            INNER JOIN zipbob_app.menu_mng m
                ON m.menu_id = l.menu_id
            WHERE l.user_device_id = :userDeviceId
            -- 최근 즐겨찾기를 먼저 표시            
            ORDER BY
                l.crated_dt DESC NULLS LAST,
                m.menu_id ASC
            """, nativeQuery = true)
    List<LikeListProjection> findFavoriteMenus(
            @Param("userDeviceId") Integer userDeviceId
    );

    // 즐겨찾기 등록: 이미 등록된 조합이면 추가하지 않는다.
    @Modifying
    @Query(value = """
            INSERT INTO zipbob_app.like_mng (
                user_device_id,
                menu_id
            )
            VALUES (
                -- : 파라미터 이름 → Java에서 전달받은 기기 ID 값이 들어갈 자리
                :userDeviceId,
                :menuId
            )
            -- PostgreSQL의 중복 처리 문법
            ON CONFLICT (user_device_id, menu_id) DO NOTHING
            """, nativeQuery = true)
    int insertIfAbsent(
            // @Param("userDeviceId") → 프론트에서 받은 값을 쿼리의 :userDeviceId에 연결
            @Param("userDeviceId") Integer userDeviceId,
            @Param("menuId") Integer menuId
    );

    // 즐겨찾기 삭제: 지정한 기기·메뉴 조합만 삭제
    @Modifying
    @Query(value = """
            DELETE FROM zipbob_app.like_mng
            WHERE user_device_id = :userDeviceId
              AND menu_id = :menuId
            """, nativeQuery = true)
    int deleteFavorite(
            @Param("userDeviceId") Integer userDeviceId,
            @Param("menuId") Integer menuId
    );
}