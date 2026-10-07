package com.zipbob.zipbob_backend.repository;

import com.zipbob.zipbob_backend.entity.Code;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface CodeRepository extends JpaRepository<Code, Long> {
    List<Code> findByCodeParentAndUseYn(String codeParent, String useYn);
}
