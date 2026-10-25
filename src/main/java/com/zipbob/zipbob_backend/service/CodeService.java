package com.zipbob.zipbob_backend.service;

import com.zipbob.zipbob_backend.dto.CodeResponseDto;
import com.zipbob.zipbob_backend.entity.Code;
import com.zipbob.zipbob_backend.repository.CodeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CodeService {
    private final CodeRepository codeRepository;

    /*codeParent, useYn으로 배열 가져오기*/
    public List<CodeResponseDto> getCategoryCodes(){
        return codeRepository
                .findByCodeParentAndUseYn("category_code","Y")
                .stream()
                .map(code -> new CodeResponseDto(code))
                .toList();
    }

    public List<CodeResponseDto> getTagCodes(){
        return codeRepository
                .findByCodeParentAndUseYn("tag_code","Y")
                .stream()
                .map(code -> new CodeResponseDto(code))
                .toList();
    }

    /*관리자용 코드 관리 카테고리, 음식특징 리스트*/
    // 카테고리 코드 목록 조회 (예: parent가 'category'인 데이터)
//    public List<Code> getAllCategoryCodes() {
//        return codeRepository.findByCodeParent("category_code"); // ERD 구조에 맞춰 parent 값 조정
//    }
//
//    // 음식 특징 코드 목록 조회 (예: parent가 'tag'인 데이터)
//    public List<Code> getAllTagCodes() {
//        return codeRepository.findByCodeParent("tag_code"); // ERD 구조에 맞춰 parent 값 조정
//    }
}
