package com.zipbob.zipbob_backend.repository;

import com.zipbob.zipbob_backend.dto.MenuListProjection;
import com.zipbob.zipbob_backend.dto.MenuTagListProjection;
import com.zipbob.zipbob_backend.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MenuFilterRepository extends JpaRepository<Menu, Integer> {

    // 기존 전체 조회용
    @Query(value = """
        SELECT 
            m.menu_id AS menuId,
            m.menu_name AS menuName,
            m.category AS category,
            zipbob_app.codename_f(m.category) AS categoryName,
            m.description AS description,
            t.tag_seq AS tagSeq,
            t.code_id AS codeId,
            zipbob_app.codename_f(t.code_id) AS tagName
        FROM zipbob_app.menu_mng m
        LEFT OUTER JOIN zipbob_app.tag_mng t
        ON m.menu_id = t.menu_id and t.use_yn = 'Y'
    """, nativeQuery = true)
    List<MenuTagListProjection> findAllWithCategoryName();

    /**
     * 카테고리 및 태그 조건에 따른 필터링 조회 예시 (Native Query)
     * 상황에 맞추어 SQL 조건절을 수정하실 수 있습니다.
     */
    @Query(value = """
        SELECT DISTINCT
            m.menu_id AS menuId,
            m.menu_name AS menuName,
            m.category AS category,
            zipbob_app.codename_f(m.category) AS categoryName,
            m.description AS description,
            f.file_path AS imageUrl
        FROM zipbob_app.menu_mng m
        LEFT JOIN zipbob_app.tag_mng t ON m.menu_id = t.menu_id AND t.use_yn = 'Y'
        LEFT JOIN file_mng f ON m.menu_id = f.menu_id AND f.use_yn = 'Y'
        WHERE (:category IS NULL OR :category = '' OR m.category = :category)
          AND (:hasTags = false OR t.code_id IN (:tags))
    """, nativeQuery = true)
    List<MenuListProjection> findFilteredMenus(
            @Param("category") String category,
            @Param("tags") List<String> tags,
            @Param("hasTags") boolean hasTags
    );
}