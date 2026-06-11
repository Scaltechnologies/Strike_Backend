package com.admin_service.vendor.repository;

import com.admin_service.vendor.entity.VendorRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VendorRecordRepository extends JpaRepository<VendorRecord, Long> {
    List<VendorRecord> findByStatus(String status);
}