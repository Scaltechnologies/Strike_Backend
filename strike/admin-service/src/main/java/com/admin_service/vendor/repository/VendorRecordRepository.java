package com.admin_service.vendor.repository;

import com.admin_service.vendor.entity.VendorRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VendorRecordRepository extends JpaRepository<VendorRecord, Long> {
    Page<VendorRecord> findByStatus(String status, Pageable pageable);
    long countByStatus(String status);
}
