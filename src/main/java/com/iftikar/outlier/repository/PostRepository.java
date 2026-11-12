package com.iftikar.outlier.repository;

import com.iftikar.outlier.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PostRepository extends JpaRepository<Post, String> {
}
