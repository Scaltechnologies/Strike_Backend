
package com.vendor_service.controller;

import com.vendor_service.dto.request.UpdateVendorProfileRequest;
import com.vendor_service.dto.response.VendorProfileResponse;
import com.vendor_service.service.VendorProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/vendor/profile")
@RequiredArgsConstructor
public class VendorProfileController {

    private final VendorProfileService vendorProfileService;

    @GetMapping
    public VendorProfileResponse getProfile(@RequestAttribute UUID vendorId) {

        return vendorProfileService.getProfile(vendorId);
    }

    @PutMapping
    public VendorProfileResponse updateProfile(
            @RequestAttribute UUID vendorId,
            @RequestBody UpdateVendorProfileRequest request) {

        return vendorProfileService.updateProfile(vendorId, request);
    }
}

