package com.iftikar.outlier.repository;

import com.iftikar.outlier.entity.PostTechnology;
import com.iftikar.outlier.entity.PostTechnologyId;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface PostTechnologyRepository
        extends JpaRepository<PostTechnology, PostTechnologyId> {

    @Query("""
            SELECT
                pt.post.id AS postId,
                pt.technology.name AS technologyName
            FROM PostTechnology pt
            WHERE pt.post.id IN :postIds
            ORDER BY pt.post.id, pt.technology.name
            """)
    List<FeedTechnologyProjection> findFeedTechnologiesByPostIds(
            @Param("postIds") List<String> postIds
    );

    interface FeedTechnologyProjection {
        String getPostId();
        String getTechnologyName();
    }
}