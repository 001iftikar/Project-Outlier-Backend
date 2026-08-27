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
        name = "post_technologies",
        indexes = {
                @Index(
                        name = "idx_post_technologies_technology_post",
                        columnList = "technology_id, post_id"
                )
        }
)
@IdClass(PostTechnologyId.class)
public class PostTechnology {

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "post_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_post_technologies_post")
    )
    private Post post;

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "technology_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_post_technologies_technology")
    )
    private Technology technology;
}