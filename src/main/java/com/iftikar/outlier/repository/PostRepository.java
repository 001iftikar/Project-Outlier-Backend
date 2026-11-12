
package com.iftikar.outlier.repository;

import com.iftikar.outlier.entity.Post;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

public interface PostRepository extends JpaRepository<Post, String> {

    // Initial feed request: newest posts first.
    @Query("""
            SELECT p.id AS postId, p.createdAt AS createdAt
            FROM Post p
            ORDER BY p.createdAt DESC, p.id DESC
            """)
    List<FeedCursorProjection> findFirstFeedWindow(Pageable pageable);


    // Subsequent feed requests: fetch posts older than the cursor.
    @Query("""
            SELECT p.id AS postId, p.createdAt AS createdAt
            FROM Post p
            WHERE p.createdAt < :createdAt
               OR (p.createdAt = :createdAt AND p.id < :postId)
            ORDER BY p.createdAt DESC, p.id DESC
            """)
    List<FeedCursorProjection> findFeedWindowAfter(
            @Param("createdAt") Instant createdAt,
            @Param("postId") String postId,
            Pageable pageable
    );


    // Fetch post and author information for an entire batch.
    @Query("""
            SELECT
                p.id AS postId,
                p.title AS title,
                p.description AS description,
                p.thumbnailUrl AS thumbnailUrl,
                p.githubUrl AS githubUrl,
                p.liveProjectUrl AS liveProjectUrl,
                p.likesCount AS likesCount,
                p.commentsCount AS commentsCount,
                p.createdAt AS createdAt,
                u.id AS authorId,
                u.username AS authorUsername,
                u.name AS authorName
            FROM Post p
            JOIN p.user u
            WHERE p.id IN :postIds
            """)
    List<FeedPostProjection> findFeedPostsByIds(
            @Param("postIds") List<String> postIds
    );


    // Fetch tags for all posts in the batch.
    @Query("""
            SELECT p.id AS postId, tag AS tag
            FROM Post p
            JOIN p.tags tag
            WHERE p.id IN :postIds
            """)
    List<FeedTagProjection> findFeedTagsByPostIds(
            @Param("postIds") List<String> postIds
    );


    interface FeedCursorProjection {
        String getPostId();
        Instant getCreatedAt();
    }

    interface FeedPostProjection {

        String getPostId();
        String getTitle();
        String getDescription();
        String getThumbnailUrl();
        String getGithubUrl();
        String getLiveProjectUrl();

        long getLikesCount();
        long getCommentsCount();

        Instant getCreatedAt();

        String getAuthorId();
        String getAuthorUsername();
        String getAuthorName();
    }

    interface FeedTagProjection {
        String getPostId();
        String getTag();
    }
}
