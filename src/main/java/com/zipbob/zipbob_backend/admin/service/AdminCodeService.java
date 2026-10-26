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
     * 공통코드 등록 및 수정 처리 (depth=1, create_id 자동 세팅, use_yn 동기화 포함)
     */
    @Transactional
    public void saveCode(CodeSaveRequestDto requestDto, String adminId) { // ★ 로그인한 관리자 ID 매개변수 추가
        // 기존에 있는 코드인지 조회
        Code code = codeRepository.findById(requestDto.getCodeId())
                .orElse(null);

        boolean isNew = (code == null);

        if (isNew) {
            // 신규 등록일 경우 객체 생성
            code = new Code();
            code.setCodeId(requestDto.getCodeId());
            code.setCodeParent(requestDto.getCodeParent());

            // ★ 요구사항 반영: depth는 기본 1, createId는 로그인한 사람
            code.setCodeDepth(1);          // 엔티티 필드명이 setDepth 또는 setDepthLvl 등인지 확인 필요
            code.setCreatedId(adminId); // 엔티티 필드명이 setCreateId 인지 확인 필요
        }

        // 공통으로 변경 가능한 값 세팅
        code.setCodeName(requestDto.getCodeName());
        code.setUseYn(requestDto.getUseYn() != null ? requestDto.getUseYn() : "N");

        // 1. 공통코드 저장 (수정 또는 등록)
        codeRepository.save(code);

        // 2. tag 테이블에서도 동일한 code_id를 찾아 use_yn 상태 동기화 갱신
        tagRepository.updateUseYnByCodeId(requestDto.getCodeId(), code.getUseYn());
    }
}