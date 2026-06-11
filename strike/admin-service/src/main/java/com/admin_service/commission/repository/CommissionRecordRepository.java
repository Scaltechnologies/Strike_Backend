package com.admin_service.commission.repository;

import com.admin_service.commission.entity.CommissionRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.util.List;

public interface CommissionRecordRepository extends JpaRepository<CommissionRecord, Long> {
    List<CommissionRecord> findByVendorId(Long vendorId);
    List<CommissionRecord> findByStatus(String status);
    List<CommissionRecord> findByVendorIdAndStatus(Long vendorId, String status);

    @Query("SELECT COALESCE(SUM(c.commissionAmount), 0) FROM CommissionRecord c WHERE c.status = :status")
    BigDecimal sumCommissionByStatus(String status);

    @Query("SELECT COALESCE(SUM(c.commissionAmount), 0) FROM CommissionRecord c WHERE c.vendorId = :vendorId")
    BigDecimal sumCommissionByVendor(Long vendorId);

    @Query("SELECT COALESCE(SUM(c.subscriptionAmount), 0) FROM CommissionRecord c")
    BigDecimal totalSubscriptionRevenue();
}