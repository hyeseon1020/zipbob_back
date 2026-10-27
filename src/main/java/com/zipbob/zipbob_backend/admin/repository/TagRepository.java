package com.zipbob.zipbob_backend.admin.repository;

import com.zipbob.zipbob_backend.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TagRepository  extends JpaRepository<Tag, Integer> {
    // Native Query로 실제 DB 테이블(tag_mng)과 컬럼명을 직접 수정
    // 공통코드 사용 상태를 태그 연결에 일괄 반영
    @Modifying(flushAutomatically = true)
    @Query(
            value = """
                    UPDATE zipbob_app.tag_mng
                    SET use_yn = :useYn
                    where Code_id = :codeId
                    """,
            nativeQuery = true
    )
    void updateUseYnByCodeId(
            @Param("codeId") String codeId,
            @Param("useYn") String useYn
    );


    // 관리자 수정 팝업용: 활성·비활성 연결 모두 조회
    @Query("""
        SELECT t.codeID
        FROM Tag t
        WHERE t.menu.menuId = :menuId
        ORDER BY t.tagSeq
        """)
    List<String> findActiveCodeIdsByMenuId(
            @Param("menuId") Integer menuId
    );

    // 특정 메뉴의 기존 태그 연결 전체 삭제
    @Modifying(flushAutomatically = true)
    @Query("""
            DELETE FROM Tag t
            WHERE t.menu.menuId = :menuId
            """)
    int deleteAllByMenuId(
            @Param("menuId") Integer menuId
    );
}