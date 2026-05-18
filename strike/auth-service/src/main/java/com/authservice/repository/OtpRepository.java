package com.authservice.repository;

import com.authservice.entity.OtpCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OtpRepository
        extends JpaRepository<OtpCode, Long> {

    Optional<OtpCode>
    findTopByMobileOrderByCreatedAtDesc(
            String mobile
    );
}