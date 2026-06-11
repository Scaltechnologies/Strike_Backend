package com.redemption_service.user.controller;

import com.redemption_service.common.dto.RedemptionResponse;
import com.redemption_service.common.response.ApiResponse;
import com.redemption_service.common.response.PageResponse;
import com.redemption_service.common.service.RedemptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/redemptions")
@RequiredArgsConstructor
public class UserRedemptionController {

    private final RedemptionService redemptionService;

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