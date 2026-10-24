package com.zipbob.zipbob_backend.repository;

import com.zipbob.zipbob_backend.entity.MenuFile;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MenuFileRepository extends JpaRepository<MenuFile, Integer> {
}