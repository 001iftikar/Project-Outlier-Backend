package com.iftikar.outlier.repository;

import com.iftikar.outlier.entity.UserTechnology;
import com.iftikar.outlier.entity.UserTechnologyId;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface UserTechnologyRepository
        extends JpaRepository<UserTechnology, UserTechnologyId> {

    @Query("""
            SELECT
                ut.user.id AS userId,
                ut.technology.name AS technologyName
            FROM UserTechnology ut
            WHERE ut.user.id IN :userIds
            ORDER BY ut.user.id, ut.technology.name
            """)
    List<UserTechnologyProjection> findTechnologiesByUserIds(
            @Param("userIds") List<String> userIds
    );

    interface UserTechnologyProjection {
        String getUserId();
        String getTechnologyName();
    }
}