package com.zipbob.zipbob_backend.admin.repository;

import com.zipbob.zipbob_backend.entity.Tag;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TagRepository  extends JpaRepository<Tag, Long> {
    // Native Query로 실제 DB 테이블(tag_mng)과 컬럼명을 직접 수정
    @Modifying(clearAutomatically = true) // 변경 후 영속성 컨텍스트를 비워주어 최신 데이터를 유지합니다
    @Query(value = "UPDATE zipbob_app.tag_mng SET use_yn = :useYn WHERE code_id = :codeId", nativeQuery = true)
    void updateUseYnByCodeId(@Param("codeId") String codeId, @Param("useYn") String useYn);
}