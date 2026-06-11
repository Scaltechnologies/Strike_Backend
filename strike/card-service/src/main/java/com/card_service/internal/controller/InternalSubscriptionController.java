package com.card_service.internal.controller;

import com.card_service.common.dto.BalanceResponse;
import com.card_service.common.dto.DeductBalanceRequest;
import com.card_service.common.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/internal/subscriptions")
@RequiredArgsConstructor
public class InternalSubscriptionController {

    private final SubscriptionService subscriptionService;

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