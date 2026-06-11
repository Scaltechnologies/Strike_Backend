package com.redemption_service.admin.controller;

import com.redemption_service.common.dto.RedemptionResponse;
import com.redemption_service.common.response.ApiResponse;
import com.redemption_service.common.service.RedemptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/redemptions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminRedemptionController {

    private final RedemptionService redemptionService;

    @GetMapping("/all")
    public ApiResponse<List<RedemptionResponse>> getAll() {
        return ApiResponse.success(redemptionService.getAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<RedemptionResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(redemptionService.getById(id));
    }

    @GetMapping("/store/{storeId}")
    public ApiResponse<List<RedemptionResponse>> getByStore(@PathVariable Long storeId) {
        return ApiResponse.success(redemptionService.getByStore(storeId));
    }

    @GetMapping("/user/{userId}")
    public ApiResponse<List<RedemptionResponse>> getByUser(@PathVariable Long userId) {
        return ApiResponse.success(redemptionService.getByUser(userId));
    }

    @GetMapping("/subscription/{subscriptionId}")
    public ApiResponse<List<RedemptionResponse>> getBySubscription(@PathVariable Long subscriptionId) {
        return ApiResponse.success(redemptionService.getBySubscription(subscriptionId));
    }
}