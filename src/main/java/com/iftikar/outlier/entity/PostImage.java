package com.iftikar.outlier.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Table(
        name = "post_images",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_post_image_position",
                        columnNames = {"post_id", "position"}
                )
        },
        indexes = {
                @Index(
                        name = "idx_post_images_post",
                        columnList = "post_id, position"
                )
        }
)
public class PostImage {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "post_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_post_images_post")
    )
    private Post post;

    @Column(nullable = false, length = 1000)
    private String imageUrl;

    @Column(nullable = false)
    private int position;
}