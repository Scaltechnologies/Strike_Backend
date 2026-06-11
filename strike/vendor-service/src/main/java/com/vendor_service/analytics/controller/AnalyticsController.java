package com.vendor_service.analytics.controller;

import com.vendor_service.analytics.dto.response.AnalyticsResponse;
import com.vendor_service.analytics.service.AnalyticsService;
import com.vendor_service.common.annotation.CurrentVendorId;
import com.vendor_service.common.response.ApiResponse;
import com.vendor_service.service.StoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('VENDOR')")
public class AnalyticsController {

    private final AnalyticsService analyticsService;
    private final StoreService storeService;

    /**
     * Analytics for the vendor's own store — no storeId needed.
     */
    @GetMapping("/my")
    public ApiResponse<AnalyticsResponse> getMyAnalytics(
            @CurrentVendorId Long vendorId) {
        Long storeId = storeService.getStoreByVendorId(vendorId).getId();
        return ApiResponse.success(analyticsService.getStoreAnalytics(storeId));
    }

    /**
     * Analytics by explicit storeId (must own the store).
     */
    @GetMapping("/store/{storeId}")
    public ApiResponse<AnalyticsResponse> getStoreAnalytics(
            @CurrentVendorId Long vendorId,
            @PathVariable Long storeId) {
        storeService.validateStoreOwnership(storeId, vendorId);
        return ApiResponse.success(analyticsService.getStoreAnalytics(storeId));
    }
}