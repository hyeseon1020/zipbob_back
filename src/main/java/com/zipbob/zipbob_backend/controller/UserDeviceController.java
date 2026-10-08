package com.zipbob.zipbob_backend.controller;

import com.zipbob.zipbob_backend.dto.UserDeviceRequestDto;
import com.zipbob.zipbob_backend.dto.UserDeviceResponseDto;
import com.zipbob.zipbob_backend.service.UserDeviceService;

import lombok.RequiredArgsConstructor;

import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/user-devices")
@RequiredArgsConstructor
public class UserDeviceController {

    private final UserDeviceService userDeviceService;

    // 기기 최초 등록 또는 기존 기기 정보 갱신
    @PostMapping
    public UserDeviceResponseDto register(
            @RequestBody UserDeviceRequestDto request
    ) {
        return userDeviceService.register(request);
    }
}