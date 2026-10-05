package com.zipbob.zipbob_backend.repository;

import com.zipbob.zipbob_backend.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

// Menu 엔티티와 Integer 타입의 PK 사용
public interface MenuRepository extends JpaRepository<Menu, Integer> {
    // 메뉴 조회 시 연결된 파일도 함께 조회. 파일이 없어도 메뉴 반환
    @Query("""
        SELECT DISTINCT m
        FROM Menu m
        LEFT JOIN FETCH m.files
        WHERE m.menuId = :menuId
        """)
    Optional<Menu> findByIdWithFiles(@Param("menuId") Integer menuId);
}