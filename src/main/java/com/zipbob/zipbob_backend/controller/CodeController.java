package com.zipbob.zipbob_backend.controller;

import com.zipbob.zipbob_backend.dto.CodeResponseDto;
import com.zipbob.zipbob_backend.service.CodeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/codes")
@RequiredArgsConstructor
public class CodeController {

    private final CodeService codeService;

    @GetMapping("/category")
    public List<CodeResponseDto> getCategoryCodes() {
        return codeService.getCategoryCodes();
    }

    @GetMapping("/tag")
    public List<CodeResponseDto> getTagCodes(){
        return codeService.getTagCodes();
    }
}
