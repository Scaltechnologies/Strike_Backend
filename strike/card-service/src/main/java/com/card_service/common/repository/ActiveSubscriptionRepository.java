package com.card_service.common.repository;

import com.card_service.common.entity.ActiveSubscription;
import com.card_service.common.enums.SubscriptionStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ActiveSubscriptionRepository extends JpaRepository<ActiveSubscription, Long> {
    List<ActiveSubscription> findByUserId(Long userId);
    List<ActiveSubscription> findByStoreId(Long storeId);
    List<ActiveSubscription> findByUserIdAndStatus(Long userId, SubscriptionStatus status);
    List<ActiveSubscription> findByStoreIdAndStatus(Long storeId, SubscriptionStatus status);
}