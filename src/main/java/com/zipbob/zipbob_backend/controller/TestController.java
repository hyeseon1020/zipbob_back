package com.zipbob.zipbob_backend.controller;

import com.zipbob.zipbob_backend.entity.Test;
import com.zipbob.zipbob_backend.service.TestService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class TestController {

    private final TestService testService;

    public TestController(TestService testService) {
        this.testService = testService;
    }

    @GetMapping("/api/test")
    public List<Test> getTest() {
        return testService.findAll();
    }
}