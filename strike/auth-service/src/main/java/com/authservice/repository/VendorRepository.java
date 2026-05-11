package com.authservice.repository;

import com.authservice.entity.Vendor;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface VendorRepository extends JpaRepository<Vendor, UUID> {

    Optional<Vendor> findByMobileNumber(String mobileNumber);

}