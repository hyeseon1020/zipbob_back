package com.zipbob.zipbob_backend.service;

import com.zipbob.zipbob_backend.entity.Test;
import com.zipbob.zipbob_backend.repository.TestRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TestService {

    private final TestRepository testRepository;

    public TestService(TestRepository testRepository) {
        this.testRepository = testRepository;
    }

    public List<Test> findAll() {
        return testRepository.findAll();
    }
}