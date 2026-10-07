package com.zipbob.zipbob_backend.service;

import com.zipbob.zipbob_backend.dto.CodeResponseDto;
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
}
