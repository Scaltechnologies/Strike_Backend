package com.card_service.common.repository;

import com.card_service.common.entity.ActiveSubscription;
import com.card_service.common.enums.SubscriptionStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface ActiveSubscriptionRepository extends JpaRepository<ActiveSubscription, Long> {
    Page<ActiveSubscription> findByUserId(Long userId, Pageable pageable);
    Page<ActiveSubscription> findByStoreId(Long storeId, Pageable pageable);
    List<ActiveSubscription> findByUserIdAndStatus(Long userId, SubscriptionStatus status);
    List<ActiveSubscription> findByStoreIdAndStatus(Long storeId, SubscriptionStatus status);

    // Returns ACTIVE subscriptions whose validity window has passed
    List<ActiveSubscription> findByStatusAndExpiresAtBefore(SubscriptionStatus status, LocalDateTime now);

    // Single-statement bulk expiry — runs after notifications are dispatched
    @Modifying
    @Query("UPDATE ActiveSubscription s SET s.status = 'EXPIRED' " +
           "WHERE s.status = 'ACTIVE' AND s.expiresAt < :now")
    int bulkExpire(@Param("now") LocalDateTime now);
}
