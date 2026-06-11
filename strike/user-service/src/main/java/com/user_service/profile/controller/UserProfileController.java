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
            @RequestHeader("X-User-Id") Long userId,
            @RequestHeader(value = "X-User-Mobile", defaultValue = "") String mobile) {
        return userProfileService.getOrCreateProfile(userId, mobile);
    }

    @PutMapping("/me")
    public UserProfileResponse updateMe(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody UserProfileRequest request) {
        return userProfileService.updateProfile(userId, request);
    }

    // ── Location ─────────────────────────────────────────────────────────────

    /**
     * Save / update the user's current GPS coordinates.
     * Frontend should call this on app open or on location change.
     * Enables GET /api/user/stores/nearby/me to work without explicit lat/lng.
     */
    @PatchMapping("/me/location")
    public UserProfileResponse updateLocation(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody UpdateLocationRequest request) {
        return userProfileService.updateLocation(userId, request.getLatitude(), request.getLongitude());
    }

    // ── Subscriptions ────────────────────────────────────────────────────────

    @PostMapping("/me/subscriptions/purchase")
    public Object purchaseSubscription(
            @RequestHeader("X-User-Id") Long userId,
            @RequestBody Object request) {
        HttpEntity<Object> entity = new HttpEntity<>(request, userHeaders(userId));
        return restTemplate.exchange(cardServiceUrl + "/api/subscriptions", HttpMethod.POST, entity, Object.class).getBody();
    }

    @GetMapping("/me/subscriptions")
    public Object getMySubscriptions(@RequestHeader("X-User-Id") Long userId) {
        return callWithUserId(cardServiceUrl + "/api/subscriptions/my", userId);
    }

    @GetMapping("/me/subscriptions/active")
    public Object getMyActiveSubscriptions(@RequestHeader("X-User-Id") Long userId) {
        return callWithUserId(cardServiceUrl + "/api/subscriptions/my/active", userId);
    }

    @GetMapping("/me/subscriptions/{id}")
    public Object getSubscriptionById(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        return callWithUserId(cardServiceUrl + "/api/subscriptions/" + id, userId);
    }

    @PatchMapping("/me/subscriptions/{id}/cancel")
    public Object cancelSubscription(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
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
    public Object getMyRedemptions(@RequestHeader("X-User-Id") Long userId) {
        return callWithUserId(redemptionServiceUrl + "/api/redemptions/user/" + userId, userId);
    }

    @GetMapping("/me/redemptions/{id}")
    public Object getRedemptionById(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        return callWithUserId(redemptionServiceUrl + "/api/redemptions/" + id, userId);
    }

    @GetMapping("/me/subscriptions/{id}/redemptions")
    public Object getRedemptionsBySubscription(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        return callWithUserId(redemptionServiceUrl + "/api/redemptions/subscription/" + id, userId);
    }

    // ── Transactions ─────────────────────────────────────────────────────────

    @GetMapping("/me/transactions")
    public Object getMyTransactions(@RequestHeader("X-User-Id") Long userId) {
        return callWithUserId(ledgerServiceUrl + "/api/ledger/user/" + userId, userId);
    }

    @GetMapping("/me/subscriptions/{id}/transactions")
    public Object getTransactionsBySubscription(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        return callWithUserId(ledgerServiceUrl + "/api/ledger/subscription/" + id, userId);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** Builds headers with USER role for downstream service calls on behalf of the user. */
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