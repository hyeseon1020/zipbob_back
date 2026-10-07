package com.zipbob.zipbob_backend.repository;

import com.zipbob.zipbob_backend.dto.MenuListProjection;
import com.zipbob.zipbob_backend.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
// 관리할 엔티티 : Menu , 타입 : Integer
public interface MenuRepository extends JpaRepository<Menu, Integer> {

    // 메뉴 레시피 조회 (메뉴 정보·이미지·인분·시간·난이도·재료·조리 순서)
    @Query("""
        SELECT DISTINCT m
        FROM Menu m
        LEFT JOIN FETCH m.files
        WHERE m.menuId = :menuId
        """)
    Optional<Menu> findByIdWithFiles(
            @Param("menuId") Integer menuId
    );

    /*
    전체 메뉴 (ID·메뉴명·카테고리·설명·이미지)
    이미지가 여러 개여도 메뉴마다 대표 이미지 한 개만 반환
    searchMenus("", "");                  // 전체 메뉴
    searchMenus("계란", "");               // 메뉴명에 '계란' 포함
    searchMenus("", "rice_bowl");         // 덮밥·비빔밥
    searchMenus("계란", "rice_bowl");      // 덮밥·비빔밥 중 '계란' 포함
    */
    //
    @Query(value = """
    SELECT
        m.menu_id AS "menuId",
        m.menu_name AS "menuName",
        m.category AS "category",
        -- m.category가 rice_bowl이면 함수가 공통코드 테이블에서 한글 이름을 찾아 반환
        zipbob_app.codename_f(m.category) AS "categoryName",
        m.description AS "description",
        (
        -- 대표 이미지 선택
            SELECT f.file_path
            FROM zipbob_app.file_mng f
            WHERE f.menu_id = m.menu_id
              AND f.use_yn = 'Y'
            ORDER BY f.file_id ASC
            LIMIT 1
        ) AS "imageUrl"
    FROM zipbob_app.menu_mng m
    -- 카테고리 조건
    WHERE (:category = '' OR m.category = :category)
    -- 검색어 조건
      AND (
          :keyword = ''
          OR LOWER(m.menu_name)
             LIKE LOWER(CONCAT('%', :keyword, '%'))
      )
    ORDER BY m.menu_id ASC
    """, nativeQuery = true)
    List<MenuListProjection> searchMenus(
            @Param("keyword") String keyword,
            @Param("category") String category
    );
}