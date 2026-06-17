package com.card_service.user.controller;

import com.card_service.common.client.VendorServiceClient;
import com.card_service.common.dto.EligibleMenuResponse;
import com.card_service.common.dto.PurchaseSubscriptionRequest;
import com.card_service.common.dto.SubscriptionResponse;
import com.card_service.common.enums.SubscriptionStatus;
import com.card_service.common.exception.BadRequestException;
import com.card_service.common.response.ApiResponse;
import com.card_service.common.response.PageResponse;
import com.card_service.common.service.CardDefinitionService;
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
    private final CardDefinitionService cardDefinitionService;
    private final VendorServiceClient vendorServiceClient;

    @PostMapping
    public ResponseEntity<?> purchase(
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody PurchaseSubscriptionRequest request) {

        Optional<ResponseEntity<?>> cached = idempotencyService.check(idempotencyKey);
        if (cached.isPresent()) return cached.get();

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            if (!idempotencyService.reserve(idempotencyKey)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error",
                                "A request with this Idempotency-Key is already being processed. Retry in a moment."));
            }
        }

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

    /**
     * Returns only the categories and items the user is eligible to redeem with this subscription.
     * Items are filtered to AVAILABLE only — out-of-stock items are excluded.
     */
    @GetMapping("/{subscriptionId}/menu")
    @PreAuthorize("hasRole('USER')")
    public ApiResponse<EligibleMenuResponse> getEligibleMenu(
            @PathVariable Long subscriptionId,
            @RequestHeader("X-User-Id") Long userId) {

        SubscriptionResponse sub = subscriptionService.getByIdForUser(subscriptionId, userId);

        if (sub.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new BadRequestException("Subscription is not active. Current status: " + sub.getStatus());
        }

        Long cardDefId = sub.getCardDefinitionId();
        List<Long> eligibleCategoryIds = cardDefinitionService.getEligibleCategoryIds(cardDefId);
        if (eligibleCategoryIds.isEmpty()) {
            throw new BadRequestException("This card has no menu categories configured. Please contact the vendor.");
        }

        List<Long> eligibleMenuItemIds = cardDefinitionService.getEligibleMenuItemIds(cardDefId);

        List<VendorServiceClient.CategoryInfo> categories;
        try {
            categories = vendorServiceClient.getCategoriesWithItems(eligibleCategoryIds);
        } catch (Exception e) {
            throw new BadRequestException("Unable to load menu. Please try again.");
        }

        List<EligibleMenuResponse.EligibleCategory> eligibleCategories = categories.stream()
                .map(cat -> EligibleMenuResponse.EligibleCategory.builder()
                        .categoryId(cat.id())
                        .categoryName(cat.name())
                        .items(cat.items().stream()
                                .filter(item -> "AVAILABLE".equals(item.availabilityStatus()))
                                .filter(item -> eligibleMenuItemIds.isEmpty()
                                        || eligibleMenuItemIds.contains(item.id()))
                                .map(item -> EligibleMenuResponse.EligibleItem.builder()
                                        .itemId(item.id())
                                        .name(item.name())
                                        .price(item.price())
                                        .itemType(item.itemType())
                                        .availabilityStatus(item.availabilityStatus())
                                        .build())
                                .toList())
                        .build())
                .filter(cat -> !cat.getItems().isEmpty())
                .toList();

        return ApiResponse.success(EligibleMenuResponse.builder()
                .subscriptionId(sub.getId())
                .cardName(sub.getCardName())
                .categories(eligibleCategories)
                .build());
    }
}