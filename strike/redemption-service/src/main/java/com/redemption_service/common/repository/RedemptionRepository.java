package com.redemption_service.common.repository;

import com.redemption_service.common.entity.RedemptionRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;

public interface RedemptionRepository extends JpaRepository<RedemptionRecord, Long> {
    Page<RedemptionRecord> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    Page<RedemptionRecord> findByStoreIdOrderByCreatedAtDesc(Long storeId, Pageable pageable);
    Page<RedemptionRecord> findBySubscriptionIdOrderByCreatedAtDesc(Long subscriptionId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(r.totalAmount), 0) FROM RedemptionRecord r")
    BigDecimal sumAllTotalAmounts();
}