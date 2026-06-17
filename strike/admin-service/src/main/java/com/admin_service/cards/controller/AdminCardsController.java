package com.admin_service.cards.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/admin/cards")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AdminCardsController {

    private final RestTemplate restTemplate;

    @Value("${services.card-url}")
    private String cardServiceUrl;

    @GetMapping("/{id}")
    public Object getCard(@PathVariable Long id) {
        return restTemplate.getForObject(
                cardServiceUrl + "/api/admin/cards/" + id, Object.class);
    }

    @GetMapping("/vendor/{vendorId}")
    public Object getCardsByVendor(@PathVariable Long vendorId) {
        return restTemplate.getForObject(
                cardServiceUrl + "/api/admin/cards/vendor/" + vendorId, Object.class);
    }

    @GetMapping("/subscriptions/store/{storeId}")
    public Object getSubscriptionsByStore(@PathVariable Long storeId) {
        return restTemplate.getForObject(
                cardServiceUrl + "/api/admin/cards/subscriptions/store/" + storeId, Object.class);
    }

    @GetMapping("/subscriptions/{id}")
    public Object getSubscription(@PathVariable Long id) {
        return restTemplate.getForObject(
                cardServiceUrl + "/api/admin/cards/subscriptions/" + id, Object.class);
    }
}