package com.iftikar.outlier.repository;

import com.iftikar.outlier.entity.Technology;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface TechnologyRepository
        extends JpaRepository<Technology, String> {

    Optional<Technology> findByNameIgnoreCase(String name);
}