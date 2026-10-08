package com.zipbob.zipbob_backend.repository;

import com.zipbob.zipbob_backend.entity.Code;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
// 관리할 엔티티 : Code , 타입 : Long
public interface CodeRepository extends JpaRepository<Code, Long> {
    //parent_code와 use_yn으로 쿼리 조회하여 리스트 가져오기
    List<Code> findByCodeParentAndUseYn(String codeParent, String useYn);
}
