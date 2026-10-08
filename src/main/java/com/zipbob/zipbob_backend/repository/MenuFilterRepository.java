package com.zipbob.zipbob_backend.repository;

import com.zipbob.zipbob_backend.dto.MenuListProjection;
import com.zipbob.zipbob_backend.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface MenuFilterRepository extends JpaRepository<Menu, Integer> {

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
    List<MenuListProjection> findAllWithCategoryName();
}