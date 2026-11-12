package com.iftikar.outlier.repository;

import com.iftikar.outlier.entity.PostTechnology;
import com.iftikar.outlier.entity.PostTechnologyId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostTechnologyRepository
        extends JpaRepository<PostTechnology, PostTechnologyId> {
}