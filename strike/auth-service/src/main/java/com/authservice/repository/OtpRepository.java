package com.authservice.repository;

import com.authservice.entity.OtpCode;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OtpRepository extends JpaRepository<OtpCode, UUID> {

    Optional<OtpCode> findTopByMobileOrderByCreatedAtDesc(String mobile);

}