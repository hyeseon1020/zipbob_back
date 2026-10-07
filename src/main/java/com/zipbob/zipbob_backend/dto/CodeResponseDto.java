package com.zipbob.zipbob_backend.dto;

import com.zipbob.zipbob_backend.entity.Code;

public class CodeResponseDto {
    private final String codeId;
    private final String codeName;


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
