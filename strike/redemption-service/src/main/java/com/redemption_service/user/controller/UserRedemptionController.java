package com.redemption_service.user.controller;

import com.redemption_service.common.dto.RedemptionRequest;
import com.redemption_service.common.dto.RedemptionResponse;
import com.redemption_service.common.response.ApiResponse;
import com.redemption_service.common.response.PageResponse;
import com.redemption_service.common.service.RedemptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/redemptions")
@RequiredArgsConstructor
public class UserRedemptionController {

    private final RedemptionService redemptionService;

    // ── Phase 5: user submits redemption request ──────────────────────────────

    @PostMapping("/request")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<ApiResponse<RedemptionResponse>> request(
            @Valid @RequestBody RedemptionRequest body) {
        Long userId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        RedemptionResponse result = redemptionService.requestRedemption(userId, body);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Redemption request submitted. Waiting for store approval.", result));
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<PageResponse<RedemptionResponse>> getByUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(redemptionService.getByUser(userId, page, size));
    }

    @GetMapping("/subscription/{subscriptionId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<PageResponse<RedemptionResponse>> getBySubscription(
            @PathVariable Long subscriptionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(redemptionService.getBySubscription(subscriptionId, page, size));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'VENDOR', 'ADMIN')")
    public ApiResponse<RedemptionResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(redemptionService.getById(id));
    }
}
