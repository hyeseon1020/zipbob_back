package com.zipbob.zipbob_backend.dto;

import com.zipbob.zipbob_backend.entity.Code;

public class CodeResponseDto {
    private final String codeId;
    private final String codeName;


    //카테고리,음식특징 코드 리스트 보내기
    public CodeResponseDto(Code code){
        this.codeId = code.getCodeId();
        this.codeName = code.getCodeName();
    }

    public String getCodeId() {
        return codeId;
    }

    public String getCodeName() {
        return codeName;
    }
}
