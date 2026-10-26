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

    // 현재 회원의 즐겨찾기 목록: 활성 카테고리 메뉴만 조회
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
        INNER JOIN zipbob_app.menu_mng m
            ON m.menu_id = l.menu_id
        WHERE l.user_id = :userId
          AND EXISTS (
              SELECT 1
              FROM zipbob_app.code_mng c
              WHERE c.code_id = m.category
                AND c.code_parent = 'category_code'
                AND c.use_yn = 'Y'
          )
        ORDER BY
            l.crated_dt DESC NULLS LAST,
            m.menu_id ASC
        """, nativeQuery = true)
    List<LikeListProjection> findFavoriteMenus(
            @Param("userId") String userId
    );

    // 이미 저장한 메뉴라면 추가하지 않음
    @Modifying
    @Query(value = """
        INSERT INTO zipbob_app.like_mng (
            user_id,
            menu_id,
            created_id
        )
        VALUES (
            :userId,
            :menuId,
            :userId
        )
        ON CONFLICT (user_id, menu_id) DO NOTHING
        """, nativeQuery = true)
    int insertIfAbsent(
            @Param("userId") String userId,
            @Param("menuId") Integer menuId
    );

    // 해당 회원의 메뉴만 삭제
    @Modifying
    @Query(value = """
        DELETE FROM zipbob_app.like_mng
        WHERE user_id = :userId
          AND menu_id = :menuId
        """, nativeQuery = true)
    int deleteFavorite(
            @Param("userId") String userId,
            @Param("menuId") Integer menuId
    );
}