package com.admin_service.user.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AdminUserController {

    private final RestTemplate restTemplate;

    @Value("${services.auth-url}")
    private String authServiceUrl;

    @Value("${services.user-url}")
    private String userServiceUrl;

    @Value("${services.card-url}")
    private String cardServiceUrl;

    @Value("${services.redemption-url}")
    private String redemptionServiceUrl;

    @Value("${services.ledger-url}")
    private String ledgerServiceUrl;

    // ── List & Detail ────────────────────────────────────────────────────────

    @GetMapping
    public Object getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return restTemplate.getForObject(
                authServiceUrl + "/internal/users?page=" + page + "&size=" + size, Object.class);
    }

    @GetMapping("/{userId}")
    public Map<String, Object> getUserDetails(@PathVariable Long userId) {
        Map<String, Object> result = new LinkedHashMap<>();

        try {
            result.put("auth", restTemplate.getForObject(
                    authServiceUrl + "/internal/users/" + userId, Object.class));
        } catch (Exception ignored) {}

        try {
            result.put("profile", restTemplate.getForObject(
                    userServiceUrl + "/internal/users/" + userId + "/profile", Object.class));
        } catch (Exception ignored) {}

        return result;
    }

    // ── User Subscriptions ───────────────────────────────────────────────────

    @GetMapping("/{userId}/subscriptions")
    public Object getUserSubscriptions(@PathVariable Long userId) {
        HttpEntity<Void> entity = new HttpEntity<>(adminHeadersFor(userId));
        return restTemplate.exchange(
                cardServiceUrl + "/api/subscriptions/my",
                HttpMethod.GET, entity, Object.class).getBody();
    }

    @GetMapping("/{userId}/subscriptions/active")
    public Object getUserActiveSubscriptions(@PathVariable Long userId) {
        HttpEntity<Void> entity = new HttpEntity<>(adminHeadersFor(userId));
        return restTemplate.exchange(
                cardServiceUrl + "/api/subscriptions/my/active",
                HttpMethod.GET, entity, Object.class).getBody();
    }

    // ── User Redemptions ─────────────────────────────────────────────────────

    @GetMapping("/{userId}/redemptions")
    public Object getUserRedemptions(@PathVariable Long userId) {
        return restTemplate.getForObject(
                redemptionServiceUrl + "/api/admin/redemptions/user/" + userId, Object.class);
    }

    // ── User Transactions ────────────────────────────────────────────────────

    @GetMapping("/{userId}/transactions")
    public Object getUserTransactions(@PathVariable Long userId) {
        return restTemplate.getForObject(
                ledgerServiceUrl + "/api/admin/ledger/user/" + userId, Object.class);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    /** Headers for admin acting on behalf of a specific user (passes ADMIN role for RBAC). */
    private HttpHeaders adminHeadersFor(Long userId) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-User-Id", userId.toString());
        headers.set("X-User-Role", "ADMIN");
        return headers;
    }

    // ── User Actions ─────────────────────────────────────────────────────────

    @PatchMapping("/{userId}/ban")
    public ResponseEntity<String> banUser(@PathVariable Long userId) {
        try {
            restTemplate.patchForObject(
                    authServiceUrl + "/internal/users/" + userId + "/ban", null, Void.class);
            return ResponseEntity.ok("User banned successfully");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Failed to ban user: " + e.getMessage());
        }
    }

    @PatchMapping("/{userId}/unban")
    public ResponseEntity<String> unbanUser(@PathVariable Long userId) {
        try {
            restTemplate.patchForObject(
                    authServiceUrl + "/internal/users/" + userId + "/unban", null, Void.class);
            return ResponseEntity.ok("User unbanned successfully");
        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Failed to unban user: " + e.getMessage());
        }
    }
}