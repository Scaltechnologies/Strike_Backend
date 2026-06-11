package com.card_service.user.controller;

import com.card_service.common.dto.PurchaseSubscriptionRequest;
import com.card_service.common.dto.SubscriptionResponse;
import com.card_service.common.response.ApiResponse;
import com.card_service.common.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
public class UserSubscriptionController {

    private final SubscriptionService subscriptionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<SubscriptionResponse> purchase(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody PurchaseSubscriptionRequest request) {
        return ApiResponse.success("Subscription purchased", subscriptionService.purchase(userId, request));
    }

    @GetMapping("/my")
    public ApiResponse<List<SubscriptionResponse>> getMySubscriptions(
            @RequestHeader("X-User-Id") Long userId) {
        return ApiResponse.success(subscriptionService.getByUser(userId));
    }

    @GetMapping("/my/active")
    public ApiResponse<List<SubscriptionResponse>> getMyActiveSubscriptions(
            @RequestHeader("X-User-Id") Long userId) {
        return ApiResponse.success(subscriptionService.getActiveByUser(userId));
    }

    @GetMapping("/{id}")
    public ApiResponse<SubscriptionResponse> getById(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        return ApiResponse.success(subscriptionService.getByIdForUser(id, userId));
    }

    @PatchMapping("/{id}/cancel")
    public ApiResponse<SubscriptionResponse> cancel(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        return ApiResponse.success("Subscription cancelled", subscriptionService.cancelSubscription(id, userId));
    }
}