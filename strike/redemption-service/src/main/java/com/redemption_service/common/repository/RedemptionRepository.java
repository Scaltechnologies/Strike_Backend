package com.redemption_service.common.repository;

import com.redemption_service.common.entity.RedemptionRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RedemptionRepository extends JpaRepository<RedemptionRecord, Long> {
    List<RedemptionRecord> findByUserIdOrderByCreatedAtDesc(Long userId);
    List<RedemptionRecord> findByStoreIdOrderByCreatedAtDesc(Long storeId);
    List<RedemptionRecord> findBySubscriptionIdOrderByCreatedAtDesc(Long subscriptionId);
}