package com.vendor_service.controller;

import com.vendor_service.common.annotation.CurrentVendorId;
import com.vendor_service.common.constants.ApiRoutes;
import com.vendor_service.common.enums.StoreStatus;
import com.vendor_service.common.response.ApiResponse;
import com.vendor_service.dto.request.StoreDetailsRequest;
import com.vendor_service.dto.request.UpdateStoreLocationRequest;
import com.vendor_service.dto.response.StoreResponse;
import com.vendor_service.service.StoreService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(ApiRoutes.Store.BASE)
@RequiredArgsConstructor
@PreAuthorize("hasRole('VENDOR')")
public class StoreController {

    private final StoreService storeService;

    @GetMapping("/my")
    public ApiResponse<StoreResponse> getMyStore(
            @CurrentVendorId Long vendorId
    ) {
        return ApiResponse.success(storeService.getStoreByVendorId(vendorId));
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