package com.redemption_service.common.repository;

import com.redemption_service.common.entity.RedemptionRecord;
import com.redemption_service.common.enums.RedemptionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public interface RedemptionRepository extends JpaRepository<RedemptionRecord, Long> {
    Page<RedemptionRecord> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
    Page<RedemptionRecord> findByStoreIdOrderByCreatedAtDesc(Long storeId, Pageable pageable);
    Page<RedemptionRecord> findBySubscriptionIdOrderByCreatedAtDesc(Long subscriptionId, Pageable pageable);

    // Vendor pending queue — oldest first so earliest requests are processed first
    List<RedemptionRecord> findByStoreIdAndStatusOrderByCreatedAtAsc(Long storeId, RedemptionStatus status);

    // Prevent duplicate pending requests for the same subscription
    boolean existsBySubscriptionIdAndStatus(Long subscriptionId, RedemptionStatus status);

    // Expiry job: find PENDING records older than the cutoff timestamp
    List<RedemptionRecord> findByStatusAndCreatedAtBefore(RedemptionStatus status, LocalDateTime cutoff);

    @Query("SELECT COALESCE(SUM(r.totalAmount), 0) FROM RedemptionRecord r")
    BigDecimal sumAllTotalAmounts();
}