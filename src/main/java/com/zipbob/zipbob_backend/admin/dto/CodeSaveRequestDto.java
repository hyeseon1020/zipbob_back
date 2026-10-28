package com.zipbob.zipbob_backend.admin.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CodeSaveRequestDto {
    private String codeId;     // 코드 ID (PK 역할)
    private String codeParent; // 부모 코드 (구분: category 등)
    private String codeName;   // 코드 이름
    private String useYn;      // 사용 여부 ('Y' 또는 'N')
    private String mode; // "create" 또는 "update"
}
