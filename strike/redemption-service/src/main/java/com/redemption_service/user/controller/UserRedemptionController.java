package com.redemption_service.user.controller;

import com.redemption_service.common.dto.RedemptionResponse;
import com.redemption_service.common.response.ApiResponse;
import com.redemption_service.common.service.RedemptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/redemptions")
@RequiredArgsConstructor
public class UserRedemptionController {

    private final RedemptionService redemptionService;

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<List<RedemptionResponse>> getByUser(@PathVariable Long userId) {
        return ApiResponse.success(redemptionService.getByUser(userId));
    }

    @GetMapping("/subscription/{subscriptionId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<List<RedemptionResponse>> getBySubscription(@PathVariable Long subscriptionId) {
        return ApiResponse.success(redemptionService.getBySubscription(subscriptionId));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('USER', 'VENDOR', 'ADMIN')")
    public ApiResponse<RedemptionResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(redemptionService.getById(id));
    }
}