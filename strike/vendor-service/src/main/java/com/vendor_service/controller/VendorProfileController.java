package com.vendor_service.controller;

import com.vendor_service.common.annotation.CurrentVendorId;
import com.vendor_service.common.response.ApiResponse;
import com.vendor_service.dto.request.UpdateVendorProfileRequest;
import com.vendor_service.dto.response.VendorProfileResponse;
import com.vendor_service.service.VendorProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/vendor/profile")
@RequiredArgsConstructor
@PreAuthorize("hasRole('VENDOR')")
public class VendorProfileController {

    private final VendorProfileService vendorProfileService;

    @GetMapping
    public ApiResponse<VendorProfileResponse> getProfile(
            @CurrentVendorId Long vendorId) {
        return ApiResponse.success(vendorProfileService.getProfile(vendorId));
    }

    /**
     * Full profile update — provide all fields you want persisted.
     * Fields not provided are left unchanged (null-safe).
     */
    @PutMapping
    public ApiResponse<VendorProfileResponse> updateProfile(
            @CurrentVendorId Long vendorId,
            @Valid @RequestBody UpdateVendorProfileRequest request) {
        return ApiResponse.success("Profile updated successfully",
                vendorProfileService.updateProfile(vendorId, request));
    }

    /**
     * Partial profile update — only provided (non-null) fields are updated.
     */
    @PatchMapping
    public ApiResponse<VendorProfileResponse> patchProfile(
            @CurrentVendorId Long vendorId,
            @Valid @RequestBody UpdateVendorProfileRequest request) {
        return ApiResponse.success("Profile updated successfully",
                vendorProfileService.updateProfile(vendorId, request));
    }

    /**
     * Check this vendor's platform approval status (PENDING, ACTIVE, SUSPENDED, REJECTED).
     * Delegates to admin-service via internal call.
     */
    @GetMapping("/status")
    public ApiResponse<Map<String, Object>> getApprovalStatus(
            @CurrentVendorId Long vendorId) {
        return ApiResponse.success(vendorProfileService.getVendorStatus(vendorId));
    }
}