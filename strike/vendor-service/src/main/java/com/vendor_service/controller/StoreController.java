package com.vendor_service.controller;

import com.vendor_service.common.annotation.CurrentVendorId;
import com.vendor_service.common.enums.StoreStatus;
import com.vendor_service.common.response.ApiResponse;
import com.vendor_service.dto.request.StoreDetailsRequest;
import com.vendor_service.dto.request.UpdateStoreLocationRequest;
import com.vendor_service.dto.response.StoreResponse;
import com.vendor_service.service.StoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/vendor/stores")
@RequiredArgsConstructor
@PreAuthorize("hasRole('VENDOR')")
public class StoreController {

    private final StoreService storeService;

    @GetMapping("/my")
    public ApiResponse<StoreResponse> getMyStore(
            @CurrentVendorId Long vendorId
    ) {
        log.info("[StoreController] GET /api/vendor/stores/my — vendorId={}", vendorId);
        StoreResponse response = storeService.getStoreByVendorId(vendorId);
        log.info("[StoreController] returning storeId={} for vendorId={}", response.getId(), vendorId);
        return ApiResponse.success(response);
    }

    @PutMapping("/my")
    public ApiResponse<StoreResponse> updateMyStore(
            @CurrentVendorId Long vendorId,
            @Valid @RequestBody StoreDetailsRequest request
    ) {
        return ApiResponse.success("Store updated successfully", storeService.updateStoreByVendorId(vendorId, request));
    }

    @PatchMapping("/my/location")
    public ApiResponse<StoreResponse> updateMyStoreLocation(
            @CurrentVendorId Long vendorId,
            @Valid @RequestBody UpdateStoreLocationRequest request
    ) {
        return ApiResponse.success("Store location updated",
                storeService.updateStoreLocation(vendorId, request.getLatitude(), request.getLongitude()));
    }

    @PatchMapping("/my/status")
    public ApiResponse<StoreResponse> updateMyStoreStatus(
            @CurrentVendorId Long vendorId,
            @RequestParam StoreStatus status
    ) {
        return ApiResponse.success("Store status updated", storeService.updateStoreStatusByVendorId(vendorId, status));
    }
}