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
        name = "technologies",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_technology_name",
                        columnNames = "name"
                )
        }
)
public class Technology {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(nullable = false, unique = true, length = 100)
    private String name;
}