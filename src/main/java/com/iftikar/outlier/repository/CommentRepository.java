package com.iftikar.outlier.repository;

import com.iftikar.outlier.entity.Comment;

import jakarta.persistence.LockModeType;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface CommentRepository
        extends JpaRepository<Comment, String> {

    // First page: newest comments first.
    @Query("""
            SELECT
                c.id AS commentId,
                c.content AS content,
                c.createdAt AS createdAt,
                c.updatedAt AS updatedAt,
                u.id AS authorId,
                u.username AS authorUsername,
                u.name AS authorName
            FROM Comment c
            JOIN c.user u
            WHERE c.post.id = :postId
            ORDER BY c.createdAt DESC, c.id DESC
            """)
    List<CommentProjection> findFirstPage(
            @Param("postId") String postId,
            Pageable pageable
    );


    // Subsequent pages: fetch comments older than the cursor.
    @Query("""
            SELECT
                c.id AS commentId,
                c.content AS content,
                c.createdAt AS createdAt,
                c.updatedAt AS updatedAt,
                u.id AS authorId,
                u.username AS authorUsername,
                u.name AS authorName
            FROM Comment c
            JOIN c.user u
            WHERE c.post.id = :postId
              AND (
                    c.createdAt < :createdAt
                    OR (
                        c.createdAt = :createdAt
                        AND c.id < :commentId
                    )
              )
            ORDER BY c.createdAt DESC, c.id DESC
            """)
    List<CommentProjection> findPageAfter(
            @Param("postId") String postId,
            @Param("createdAt") Instant createdAt,
            @Param("commentId") String commentId,
            Pageable pageable
    );


    // Lock a comment before editing or deleting it.
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("""
            SELECT c
            FROM Comment c
            WHERE c.id = :commentId
              AND c.post.id = :postId
            """)
    Optional<Comment> findByIdForUpdate(
            @Param("commentId") String commentId,
            @Param("postId") String postId
    );


    interface CommentProjection {

        String getCommentId();

        String getContent();

        Instant getCreatedAt();

        Instant getUpdatedAt();

        String getAuthorId();

        String getAuthorUsername();

        String getAuthorName();
    }
}