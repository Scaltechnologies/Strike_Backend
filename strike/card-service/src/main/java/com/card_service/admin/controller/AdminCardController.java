package com.card_service.admin.controller;

import com.card_service.common.dto.CardDefinitionResponse;
import com.card_service.common.dto.SubscriptionResponse;
import com.card_service.common.response.ApiResponse;
import com.card_service.common.service.CardDefinitionService;
import com.card_service.common.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/cards")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCardController {

    private final CardDefinitionService cardService;
    private final SubscriptionService subscriptionService;

    @GetMapping("/{id}")
    public ApiResponse<CardDefinitionResponse> getCard(@PathVariable Long id) {
        return ApiResponse.success(cardService.getCardById(id));
    }

    @GetMapping("/vendor/{vendorId}")
    public ApiResponse<List<CardDefinitionResponse>> getCardsByVendor(@PathVariable Long vendorId) {
        return ApiResponse.success(cardService.getCardsByVendor(vendorId));
    }

    @GetMapping("/subscriptions/store/{storeId}")
    public ApiResponse<List<SubscriptionResponse>> getSubscriptionsByStore(@PathVariable Long storeId) {
        return ApiResponse.success(subscriptionService.getByStore(storeId));
    }

    @GetMapping("/subscriptions/{id}")
    public ApiResponse<SubscriptionResponse> getSubscription(@PathVariable Long id) {
        return ApiResponse.success(subscriptionService.getById(id));
    }
}