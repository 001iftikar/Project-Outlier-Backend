package com.iftikar.outlier.repository;

import com.iftikar.outlier.entity.PostImage;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostImageRepository
        extends JpaRepository<PostImage, String> {
}