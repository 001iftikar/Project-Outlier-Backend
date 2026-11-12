package com.iftikar.outlier.repository;

import com.iftikar.outlier.entity.PostImage;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostImageRepository
        extends JpaRepository<PostImage, String> {

    @Query("""
            SELECT
                pi.post.id AS postId,
                pi.imageUrl AS imageUrl,
                pi.position AS position
            FROM PostImage pi
            WHERE pi.post.id IN :postIds
            ORDER BY pi.post.id, pi.position
            """)
    List<FeedImageProjection> findFeedImagesByPostIds(
            @Param("postIds") List<String> postIds
    );

    interface FeedImageProjection {
        String getPostId();
        String getImageUrl();
        int getPosition();
    }
}