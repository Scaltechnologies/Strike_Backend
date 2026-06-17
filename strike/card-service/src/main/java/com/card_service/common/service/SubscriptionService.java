package com.card_service.common.service;

import com.card_service.common.dto.BalanceResponse;
import com.card_service.common.dto.PurchaseSubscriptionRequest;
import com.card_service.common.dto.SubscriptionResponse;
import com.card_service.common.response.PageResponse;

import java.math.BigDecimal;
import java.util.List;

public interface SubscriptionService {
    SubscriptionResponse purchase(Long userId, PurchaseSubscriptionRequest request);
    SubscriptionResponse getById(Long id);
    SubscriptionResponse getByIdForUser(Long id, Long userId);
    PageResponse<SubscriptionResponse> getByUser(Long userId, int page, int size);
    List<SubscriptionResponse> getActiveByUser(Long userId);
    PageResponse<SubscriptionResponse> getByStore(Long storeId, int page, int size);
    BalanceResponse getBalance(Long subscriptionId);
    BalanceResponse deductBalance(Long subscriptionId, BigDecimal amount);
    SubscriptionResponse cancelSubscription(Long id, Long userId);

    Long getCardDefinitionId(Long subscriptionId);

    /**
     * Marks all ACTIVE subscriptions whose expiresAt has passed as EXPIRED.
     * Called by the scheduled job and the internal ops endpoint.
     *
     * @return number of subscriptions expired in this run
     */
    int expireOverdueSubscriptions();
}
