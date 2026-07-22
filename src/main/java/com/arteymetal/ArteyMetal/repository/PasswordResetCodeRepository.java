package com.arteymetal.ArteyMetal.repository;

import com.arteymetal.ArteyMetal.entity.PasswordResetCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface PasswordResetCodeRepository extends JpaRepository<PasswordResetCode, Long> {
    PasswordResetCode findTopByEmailOrderByCreatedAtDesc(String email);
    void deleteByEmail(String email);
    PasswordResetCode findTopByEmailAndCodeAndExpiresAtAfter(String email, String code, LocalDateTime now);
}
