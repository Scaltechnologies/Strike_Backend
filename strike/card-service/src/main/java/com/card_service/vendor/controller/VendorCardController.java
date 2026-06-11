package com.card_service.vendor.controller;

import com.card_service.common.dto.CardDefinitionResponse;
import com.card_service.common.dto.CreateCardRequest;
import com.card_service.common.dto.SubscriptionResponse;
import com.card_service.common.dto.UpdateCardRequest;
import com.card_service.common.response.ApiResponse;
import com.card_service.common.response.PageResponse;
import com.card_service.common.service.CardDefinitionService;
import com.card_service.common.service.SubscriptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
@PreAuthorize("hasRole('VENDOR')")
public class VendorCardController {

    private final CardDefinitionService cardService;
    private final SubscriptionService subscriptionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CardDefinitionResponse> createCard(
            @RequestHeader("X-User-Id") Long vendorId,
            @Valid @RequestBody CreateCardRequest request) {
        return ApiResponse.success("Card created successfully", cardService.createCard(vendorId, request));
    }

    @GetMapping("/my")
    public ApiResponse<List<CardDefinitionResponse>> getMyCards(
            @RequestHeader("X-User-Id") Long vendorId) {
        return ApiResponse.success(cardService.getCardsByVendor(vendorId));
    }

    @PutMapping("/{id}")
    public ApiResponse<CardDefinitionResponse> updateCard(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long vendorId,
            @Valid @RequestBody UpdateCardRequest request) {
        return ApiResponse.success("Card updated", cardService.updateCard(id, vendorId, request));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deactivateCard(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long vendorId) {
        cardService.deactivateCard(id, vendorId);
    }

    @GetMapping("/subscriptions/store/{storeId}")
    public ApiResponse<PageResponse<SubscriptionResponse>> getSubscriptionsByStore(
            @PathVariable Long storeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(subscriptionService.getByStore(storeId, page, size));
    }

    @GetMapping("/subscriptions/{subscriptionId}")
    public ApiResponse<SubscriptionResponse> getSubscriptionById(@PathVariable Long subscriptionId) {
        return ApiResponse.success(subscriptionService.getById(subscriptionId));
    }
}