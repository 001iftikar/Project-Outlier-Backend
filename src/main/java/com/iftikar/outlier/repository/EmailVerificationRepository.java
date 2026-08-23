package com.iftikar.outlier.repository;

import com.iftikar.outlier.entity.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface EmailVerificationRepository
        extends JpaRepository<EmailVerification, String> {

    Optional<EmailVerification> findByEmail(String email);

    void deleteByEmail(String email);
}