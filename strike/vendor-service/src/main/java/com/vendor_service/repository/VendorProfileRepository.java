
package com.vendor_service.repository;

import com.vendor_service.entity.VendorProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface VendorProfileRepository extends JpaRepository<VendorProfile, UUID> {
}

