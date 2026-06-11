package com.vendor_service.dashboard.controller;

import com.vendor_service.common.annotation.CurrentVendorId;
import com.vendor_service.common.constants.ApiRoutes;
import com.vendor_service.common.response.ApiResponse;
import com.vendor_service.dashboard.dto.response.DashboardSummaryResponse;
import com.vendor_service.dashboard.service.DashboardService;
import com.vendor_service.service.StoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiRoutes.Dashboard.BASE)
@RequiredArgsConstructor
@PreAuthorize("hasRole('VENDOR')")
public class DashboardController {

    private final DashboardService dashboardService;
    private final StoreService storeService;

    /**
     * Vendor dashboard using their own store — no need to know storeId.
     */
    @GetMapping("/my")
    public ApiResponse<DashboardSummaryResponse> getMyDashboard(
            @CurrentVendorId Long vendorId) {
        Long storeId = storeService.getStoreByVendorId(vendorId).getId();
        return ApiResponse.success(dashboardService.getDashboardSummary(storeId));
    }

    /**
     * Vendor dashboard by explicit storeId (must own the store).
     */
    @GetMapping("/store/{storeId}")
    public ApiResponse<DashboardSummaryResponse> getDashboardSummary(
            @CurrentVendorId Long vendorId,
            @PathVariable Long storeId) {
        storeService.validateStoreOwnership(storeId, vendorId);
        return ApiResponse.success(dashboardService.getDashboardSummary(storeId));
    }
}