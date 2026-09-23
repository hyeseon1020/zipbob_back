package com.zipbob.zipbob_backend.repository;

import com.zipbob.zipbob_backend.entity.Test;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TestRepository extends JpaRepository<Test, String> {

}