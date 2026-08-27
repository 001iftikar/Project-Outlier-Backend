package com.iftikar.outlier.entity;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class PostTechnologyId implements Serializable {

    private String post;
    private String technology;
}