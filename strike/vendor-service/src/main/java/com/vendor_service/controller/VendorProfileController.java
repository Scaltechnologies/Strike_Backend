package com.vendor_service.controller;

import com.vendor_service.dto.request.UpdateVendorProfileRequest;
import com.vendor_service.dto.response.VendorProfileResponse;
import com.vendor_service.service.VendorProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/vendor/profile")
@RequiredArgsConstructor
public class VendorProfileController {

    private final VendorProfileService vendorProfileService;

    @PutMapping("/{vendorId}")
    public ResponseEntity<VendorProfileResponse>
    updateProfile(
            @PathVariable Long vendorId,
            @RequestBody UpdateVendorProfileRequest request
    ) {

        return ResponseEntity.ok(
                vendorProfileService.updateProfile(
                        vendorId,
                        request
                )
        );
    }

    @GetMapping("/{vendorId}")
    public ResponseEntity<VendorProfileResponse>
    getProfile(
            @PathVariable Long vendorId
    ) {

        return ResponseEntity.ok(
                vendorProfileService.getProfile(vendorId)
        );
    }
}