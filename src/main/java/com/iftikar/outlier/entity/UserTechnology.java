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
        name = "user_technologies",
        indexes = {
                @Index(
                        name = "idx_user_technologies_technology_user",
                        columnList = "technology_id, user_id"
                )
        }
)
@IdClass(UserTechnologyId.class)
public class UserTechnology {

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_technologies_user")
    )
    private User user;

    @Id
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "technology_id",
            nullable = false,
            foreignKey = @ForeignKey(name = "fk_user_technologies_technology")
    )
    private Technology technology;
}