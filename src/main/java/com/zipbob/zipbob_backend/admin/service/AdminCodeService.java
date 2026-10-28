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
        // ★ 프론트에서 넘어온 mode 값으로 명확하게 분기
        if ("update".equals(requestDto.getMode())) {
            // [수정 모드]
            Code existingCode = codeRepository.findById(requestDto.getCodeId())
                    .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 코드입니다."));

            existingCode.setCodeName(requestDto.getCodeName());
            existingCode.setUseYn(requestDto.getUseYn() != null ? requestDto.getUseYn() : "N");
            //공통코드 저장 (수정)
            codeRepository.save(existingCode);

        } else {
            // [신규 등록 모드] 이미 존재하는 ID인지 체크
            if (codeRepository.existsById(requestDto.getCodeId())) {
                throw new IllegalArgumentException("이미 존재하는 코드 ID입니다.");
            }

            Code code = new Code();
            code.setCodeId(requestDto.getCodeId());
            code.setCodeParent(requestDto.getCodeParent());

            // depth는 기본 1, createId는 로그인한 사람
            code.setCodeDepth(1);          // 기본 depth 1
            code.setCreatedId(adminId);   // 로그인한 관리자 ID
            code.setCodeName(requestDto.getCodeName());
            code.setUseYn(requestDto.getUseYn() != null ? requestDto.getUseYn() : "N");
            //공통코드 저장 (등록)
            codeRepository.save(code);
        }
        // 태그 테이블 use_yn 동기화
        tagRepository.updateUseYnByCodeId(requestDto.getCodeId(), requestDto.getUseYn() != null ? requestDto.getUseYn() : "N");
    }
}