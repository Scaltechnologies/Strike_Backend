package com.authservice.repository;

import com.authservice.entity.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VendorRepository
        extends JpaRepository<Vendor, Long> {

    Optional<Vendor> findByMobileNumber(
            String mobileNumber
    );
}