package com.admin_service.redemptions.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/admin/redemptions")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AdminRedemptionsController {

    private final RestTemplate restTemplate;

    @Value("${services.redemption-url}")
    private String redemptionServiceUrl;

    @GetMapping("/all")
    public Object getAll() {
        return restTemplate.getForObject(
                redemptionServiceUrl + "/api/admin/redemptions/all", Object.class);
    }

    @GetMapping("/{id}")
    public Object getById(@PathVariable Long id) {
        return restTemplate.getForObject(
                redemptionServiceUrl + "/api/admin/redemptions/" + id, Object.class);
    }

    @GetMapping("/store/{storeId}")
    public Object getByStore(@PathVariable Long storeId) {
        return restTemplate.getForObject(
                redemptionServiceUrl + "/api/admin/redemptions/store/" + storeId, Object.class);
    }

    @GetMapping("/user/{userId}")
    public Object getByUser(@PathVariable Long userId) {
        return restTemplate.getForObject(
                redemptionServiceUrl + "/api/admin/redemptions/user/" + userId, Object.class);
    }

    @GetMapping("/subscription/{subscriptionId}")
    public Object getBySubscription(@PathVariable Long subscriptionId) {
        return restTemplate.getForObject(
                redemptionServiceUrl + "/api/admin/redemptions/subscription/" + subscriptionId, Object.class);
    }
}