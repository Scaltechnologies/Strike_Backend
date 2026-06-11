package com.authservice.controller;

import com.authservice.service.VendorAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/vendors")
@RequiredArgsConstructor
public class InternalVendorController {

    private final VendorAuthService vendorAuthService;

    @PatchMapping("/{vendorId}/approve")
    public ResponseEntity<Void> approveVendor(@PathVariable Long vendorId) {
        vendorAuthService.approveVendor(vendorId);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{vendorId}/reject")
    public ResponseEntity<Void> rejectVendor(@PathVariable Long vendorId) {
        vendorAuthService.rejectVendor(vendorId);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{vendorId}/suspend")
    public ResponseEntity<Void> suspendVendor(@PathVariable Long vendorId) {
        vendorAuthService.suspendVendor(vendorId);
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{vendorId}/reactivate")
    public ResponseEntity<Void> reactivateVendor(@PathVariable Long vendorId) {
        vendorAuthService.reactivateVendor(vendorId);
        return ResponseEntity.ok().build();
    }
}