package com.card_service.internal.controller;

import com.card_service.common.dto.BalanceResponse;
import com.card_service.common.dto.DeductBalanceRequest;
import com.card_service.common.dto.SubscriptionRedemptionContext;
import com.card_service.common.dto.SubscriptionResponse;
import com.card_service.common.service.CardDefinitionService;
import com.card_service.common.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/internal/subscriptions")
@RequiredArgsConstructor
public class InternalSubscriptionController {

    private final SubscriptionService subscriptionService;
    private final CardDefinitionService cardDefinitionService;

    @GetMapping("/{id}/balance")
    public BalanceResponse getBalance(@PathVariable Long id) {
        return subscriptionService.getBalance(id);
    }

    @PostMapping("/{id}/deduct")
    public BalanceResponse deductBalance(
            @PathVariable Long id,
            @Valid @RequestBody DeductBalanceRequest request) {
        return subscriptionService.deductBalance(id, request.getAmount());
    }

    @GetMapping("/{id}/eligible-category-ids")
    public List<Long> getEligibleCategoryIds(@PathVariable Long id) {
        Long cardDefinitionId = subscriptionService.getCardDefinitionId(id);
        return cardDefinitionService.getEligibleCategoryIds(cardDefinitionId);
    }

    /**
     * Single call that gives the redemption-service everything it needs:
     * the subscription owner, the store it was purchased for, the status,
     * and the category IDs the card is allowed to redeem.
     */
    @GetMapping("/{id}/redemption-context")
    public SubscriptionRedemptionContext getRedemptionContext(@PathVariable Long id) {
        SubscriptionResponse sub = subscriptionService.getById(id);
        Long cardDefId = sub.getCardDefinitionId();
        List<Long> eligibleCategoryIds = cardDefinitionService.getEligibleCategoryIds(cardDefId);
        List<Long> eligibleMenuItemIds = cardDefinitionService.getEligibleMenuItemIds(cardDefId);
        return SubscriptionRedemptionContext.builder()
                .userId(sub.getUserId())
                .storeId(sub.getStoreId())
                .cardDefinitionId(cardDefId)
                .status(sub.getStatus().name())
                .eligibleCategoryIds(eligibleCategoryIds)
                .eligibleMenuItemIds(eligibleMenuItemIds)
                .build();
    }

    // Ops endpoint: manually trigger the expiry sweep (e.g. for testing or catch-up after downtime)
    @PostMapping("/expire")
    public ResponseEntity<Map<String, Object>> triggerExpiry() {
        int expired = subscriptionService.expireOverdueSubscriptions();
        return ResponseEntity.ok(Map.of(
                "message", "Expiry sweep completed",
                "expiredCount", expired
        ));
    }
}