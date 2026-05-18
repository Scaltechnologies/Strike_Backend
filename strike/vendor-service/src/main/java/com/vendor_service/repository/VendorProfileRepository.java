package com.vendor_service.repository;

import com.vendor_service.entity.VendorProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VendorProfileRepository
        extends JpaRepository<VendorProfile, Long> {

    Optional<VendorProfile> findByVendorId(Long vendorId);
}