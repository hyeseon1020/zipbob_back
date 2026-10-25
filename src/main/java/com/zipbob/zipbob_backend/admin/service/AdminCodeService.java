package com.zipbob.zipbob_backend.admin.service;

import com.zipbob.zipbob_backend.admin.dto.CodeSaveRequestDto;
import com.zipbob.zipbob_backend.admin.repository.TagRepository;
import com.zipbob.zipbob_backend.entity.Code;
import com.zipbob.zipbob_backend.repository.CodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminCodeService {

    private final CodeRepository codeRepository;
    private final TagRepository tagRepository;

    // 카테고리 코드 목록 조회
    public List<Code> getCategoryCodes() {
        return codeRepository.findByCodeParent("category_code");
    }

    // 음식 특징 코드 목록 조회
    public List<Code> getTagCodes() {
        return codeRepository.findByCodeParent("tag_code");
    }

    /**
     * 공통코드 등록 및 수정 처리 (use_yn 동기화 포함)
     */
    @Transactional
    public void saveCode(CodeSaveRequestDto requestDto) {
        // 이미 존재하는 코드인지 확인 (ID 기준)
        Code code = codeRepository.findById(requestDto.getCodeId()).orElse(new Code()); // 없으면 새로 생성

        // 엔티티 구조에 맞춰 값 설정
        code.setCodeId(requestDto.getCodeId());
        code.setCodeParent(requestDto.getCodeParent());
        code.setCodeName(requestDto.getCodeName());
        code.setUseYn(requestDto.getUseYn() != null ? requestDto.getUseYn() : "N");

        // 1. 공통코드 저장 (수정 또는 등록)
        codeRepository.save(code);

        // 2. tag 테이블에서도 동일한 code_id를 찾아 use_yn 상태 동기화 갱신
        tagRepository.updateUseYnByCodeId(requestDto.getCodeId(), code.getUseYn());
    }
}