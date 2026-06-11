package com.card_service.internal.controller;

import com.card_service.common.dto.BalanceResponse;
import com.card_service.common.dto.DeductBalanceRequest;
import com.card_service.common.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

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
}