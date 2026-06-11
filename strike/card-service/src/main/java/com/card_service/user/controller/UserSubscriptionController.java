package com.card_service.user.controller;

import com.card_service.common.dto.PurchaseSubscriptionRequest;
import com.card_service.common.dto.SubscriptionResponse;
import com.card_service.common.response.ApiResponse;
import com.card_service.common.response.PageResponse;
import com.card_service.common.service.IdempotencyService;
import com.card_service.common.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/subscriptions")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('USER', 'ADMIN')")
public class UserSubscriptionController {

    private final SubscriptionService subscriptionService;
    private final IdempotencyService idempotencyService;

    @PostMapping
    public ResponseEntity<?> purchase(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody PurchaseSubscriptionRequest request) {

        // 1. Return cached response or 409 if already in-flight
        Optional<ResponseEntity<?>> cached = idempotencyService.check(idempotencyKey);
        if (cached.isPresent()) return cached.get();

        // 2. Reserve key before processing to prevent concurrent duplicates
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            if (!idempotencyService.reserve(idempotencyKey)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error",
                                "A request with this Idempotency-Key is already being processed. Retry in a moment."));
            }
        }

        // 3. Process — cancel reservation on failure so client can retry
        try {
            ApiResponse<SubscriptionResponse> result = ApiResponse.success(
                    "Subscription purchased", subscriptionService.purchase(userId, request));
            idempotencyService.complete(idempotencyKey, result, HttpStatus.CREATED.value());
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (RuntimeException e) {
            idempotencyService.cancel(idempotencyKey);
            throw e;
        }
    }

    @GetMapping("/my")
    public ApiResponse<PageResponse<SubscriptionResponse>> getMySubscriptions(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(subscriptionService.getByUser(userId, page, size));
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