package com.admin_service.ledger.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/admin/ledger")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AdminLedgerController {

    private final RestTemplate restTemplate;

    @Value("${services.ledger-url}")
    private String ledgerServiceUrl;

    @GetMapping("/all")
    public Object getAll() {
        return restTemplate.getForObject(
                ledgerServiceUrl + "/api/admin/ledger/all", Object.class);
    }

    @GetMapping("/subscription/{subscriptionId}")
    public Object getBySubscription(@PathVariable Long subscriptionId) {
        return restTemplate.getForObject(
                ledgerServiceUrl + "/api/admin/ledger/subscription/" + subscriptionId, Object.class);
    }

    @GetMapping("/store/{storeId}")
    public Object getByStore(@PathVariable Long storeId) {
        return restTemplate.getForObject(
                ledgerServiceUrl + "/api/admin/ledger/store/" + storeId, Object.class);
    }

    @GetMapping("/user/{userId}")
    public Object getByUser(@PathVariable Long userId) {
        return restTemplate.getForObject(
                ledgerServiceUrl + "/api/admin/ledger/user/" + userId, Object.class);
    }
}