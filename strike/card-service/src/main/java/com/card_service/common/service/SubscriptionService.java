package com.card_service.common.service;

import com.card_service.common.dto.BalanceResponse;
import com.card_service.common.dto.PurchaseSubscriptionRequest;
import com.card_service.common.dto.SubscriptionResponse;

import java.math.BigDecimal;
import java.util.List;

public interface SubscriptionService {
    SubscriptionResponse purchase(Long userId, PurchaseSubscriptionRequest request);
    SubscriptionResponse getById(Long id);
    SubscriptionResponse getByIdForUser(Long id, Long userId);
    List<SubscriptionResponse> getByUser(Long userId);
    List<SubscriptionResponse> getActiveByUser(Long userId);
    List<SubscriptionResponse> getByStore(Long storeId);
    BalanceResponse getBalance(Long subscriptionId);
    BalanceResponse deductBalance(Long subscriptionId, BigDecimal amount);
    SubscriptionResponse cancelSubscription(Long id, Long userId);
}