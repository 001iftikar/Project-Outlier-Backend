package com.iftikar.outlier.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "posts",
        indexes = {
                @Index(
                        name = "idx_posts_user_created",
                        columnList = "user_id, created_at"
                ),
                @Index(
                        name = "idx_posts_feed_created_id",
                        columnList = "created_at, id"
                )
        }
)
public class Post {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_posts_user")
    )
    private User user;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "thumbnail_url", nullable = false, length = 1000)
    private String thumbnailUrl;

    @Column(name = "github_url", length = 500)
    private String githubUrl;

    @Column(name = "live_project_url", length = 500)
    private String liveProjectUrl;

    @Column(name = "likes_count", nullable = false)
    @Builder.Default
    private long likesCount = 0;

    @Column(name = "comments_count", nullable = false)
    @Builder.Default
    private long commentsCount = 0;
    @ElementCollection
    @CollectionTable(
            name = "post_tags",
            joinColumns = @JoinColumn(
                    name = "post_id",
                    foreignKey = @ForeignKey(name = "fk_post_tags_post")
            )
    )
    @Column(name = "tag", nullable = false, length = 50)
    @Builder.Default
    private List<String> tags = new ArrayList<>();

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant updatedAt;

    @PrePersist
    protected void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = Instant.now();
    }
}
