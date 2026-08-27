package com.iftikar.outlier.entity;

import lombok.*;

import java.io.Serializable;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class UserTechnologyId implements Serializable {

    private String user;
    private String technology;
}