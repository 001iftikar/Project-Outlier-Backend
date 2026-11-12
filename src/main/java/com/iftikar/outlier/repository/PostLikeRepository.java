package com.iftikar.outlier.repository;

import com.iftikar.outlier.entity.PostLike;
import com.iftikar.outlier.entity.PostLikeId;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostLikeRepository
        extends JpaRepository<PostLike, PostLikeId> {

    @Query("""
            SELECT pl.post.id
            FROM PostLike pl
            WHERE pl.user.id = :userId
              AND pl.post.id IN :postIds
            """)
    List<String> findLikedPostIds(
            @Param("userId") String userId,
            @Param("postIds") List<String> postIds
    );
}