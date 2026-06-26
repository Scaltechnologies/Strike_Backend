package com.user_service.profile.controller;

import com.user_service.profile.dto.UpdateLocationRequest;
import com.user_service.profile.dto.UserProfileRequest;
import com.user_service.profile.dto.UserProfileResponse;
import com.user_service.profile.service.UserProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;
    private final RestTemplate restTemplate;

    @Value("${services.card-url}")
    private String cardServiceUrl;

    @Value("${services.redemption-url}")
    private String redemptionServiceUrl;

    @Value("${services.ledger-url}")
    private String ledgerServiceUrl;

    // ── Profile ──────────────────────────────────────────────────────────────

    @GetMapping("/me")
    public UserProfileResponse getMe(
            @RequestHeader(value = "X-User-Mobile", defaultValue = "") String mobile) {
        Long userId = principalId();
        return userProfileService.getOrCreateProfile(userId, mobile);
    }

    @PutMapping("/me")
    public UserProfileResponse updateMe(@Valid @RequestBody UserProfileRequest request) {
        return userProfileService.updateProfile(principalId(), request);
    }

    // ── Location ─────────────────────────────────────────────────────────────

    @PatchMapping("/me/location")
    public UserProfileResponse updateLocation(@Valid @RequestBody UpdateLocationRequest request) {
        return userProfileService.updateLocation(principalId(), request.getLatitude(), request.getLongitude());
    }

    // ── Subscriptions ────────────────────────────────────────────────────────

    @PostMapping("/me/subscriptions/purchase")
    public Object purchaseSubscription(@RequestBody Object request) {
        HttpEntity<Object> entity = new HttpEntity<>(request, userHeaders(principalId()));
        return restTemplate.exchange(cardServiceUrl + "/api/subscriptions", HttpMethod.POST, entity, Object.class).getBody();
    }

    @GetMapping("/me/subscriptions")
    public Object getMySubscriptions() {
        return callWithUserId(cardServiceUrl + "/api/subscriptions/my", principalId());
    }

    @GetMapping("/me/subscriptions/active")
    public Object getMyActiveSubscriptions() {
        return callWithUserId(cardServiceUrl + "/api/subscriptions/my/active", principalId());
    }

    @GetMapping("/me/subscriptions/{id}")
    public Object getSubscriptionById(@PathVariable Long id) {
        return callWithUserId(cardServiceUrl + "/api/subscriptions/" + id, principalId());
    }

    @PatchMapping("/me/subscriptions/{id}/cancel")
    public Object cancelSubscription(@PathVariable Long id) {
        Long userId = principalId();
        HttpEntity<Void> entity = new HttpEntity<>(userHeaders(userId));
        return restTemplate.exchange(
                cardServiceUrl + "/api/subscriptions/" + id + "/cancel",
                HttpMethod.PATCH, entity, Object.class).getBody();
    }

    // ── Store cards ──────────────────────────────────────────────────────────

    @GetMapping("/stores/{storeId}/cards")
    public Object getStoreCards(@PathVariable Long storeId) {
        return restTemplate.getForObject(cardServiceUrl + "/api/cards/store/" + storeId, Object.class);
    }

    // ── Redemptions ──────────────────────────────────────────────────────────

    @GetMapping("/me/redemptions")
    public Object getMyRedemptions() {
        Long userId = principalId();
        return callWithUserId(redemptionServiceUrl + "/api/redemptions/user/" + userId, userId);
    }

    @GetMapping("/me/redemptions/{id}")
    public Object getRedemptionById(@PathVariable Long id) {
        return callWithUserId(redemptionServiceUrl + "/api/redemptions/" + id, principalId());
    }

    @GetMapping("/me/subscriptions/{id}/redemptions")
    public Object getRedemptionsBySubscription(@PathVariable Long id) {
        return callWithUserId(redemptionServiceUrl + "/api/redemptions/subscription/" + id, principalId());
    }

    // ── Transactions ─────────────────────────────────────────────────────────

    @GetMapping("/me/transactions")
    public Object getMyTransactions() {
        Long userId = principalId();
        return callWithUserId(ledgerServiceUrl + "/api/ledger/user/" + userId, userId);
    }

    @GetMapping("/me/subscriptions/{id}/transactions")
    public Object getTransactionsBySubscription(@PathVariable Long id) {
        return callWithUserId(ledgerServiceUrl + "/api/ledger/subscription/" + id, principalId());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Long principalId() {
        return (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    }

    private HttpHeaders userHeaders(Long userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", userId.toString());
        headers.set("X-User-Role", "USER");
        return headers;
    }

    private Object callWithUserId(String url, Long userId) {
        HttpEntity<Void> entity = new HttpEntity<>(userHeaders(userId));
        return restTemplate.exchange(url, HttpMethod.GET, entity, Object.class).getBody();
    }
}