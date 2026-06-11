package com.admin_service.vendor.controller;

import com.admin_service.vendor.dto.VendorRegistrationRequest;
import com.admin_service.vendor.entity.VendorRecord;
import com.admin_service.vendor.repository.VendorRecordRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/internal/admin/vendors")
@RequiredArgsConstructor
public class InternalAdminVendorController {

    private final VendorRecordRepository vendorRecordRepository;

    /**
     * Called by auth-service when a vendor completes registration.
     * Creates or returns existing vendor record (idempotent).
     */
    @PostMapping
    public ResponseEntity<Void> upsertVendorRecord(
            @Valid @RequestBody VendorRegistrationRequest request) {
        vendorRecordRepository.findById(request.getVendorId())
                .orElseGet(() -> vendorRecordRepository.save(
                        VendorRecord.builder()
                                .vendorId(request.getVendorId())
                                .hotelName(request.getHotelName())
                                .mobileNumber(request.getMobileNumber())
                                .email(request.getEmail())
                                .status("PENDING")
                                .build()
                ));
        return ResponseEntity.ok().build();
    }

    /**
     * Called by vendor-service to retrieve a vendor's full record.
     */
    @GetMapping("/{vendorId}")
    public ResponseEntity<VendorRecord> getVendorRecord(
            @PathVariable Long vendorId) {
        return vendorRecordRepository.findById(vendorId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Called by vendor-service so vendors can check their own approval status.
     * Returns: status, rejectionReason, commissionRate.
     */
    @GetMapping("/{vendorId}/status")
    public ResponseEntity<Map<String, Object>> getVendorStatus(
            @PathVariable Long vendorId) {
        return vendorRecordRepository.findById(vendorId)
                .map(v -> {
                    Map<String, Object> result = new LinkedHashMap<>();
                    result.put("vendorId", v.getVendorId());
                    result.put("status", v.getStatus());
                    result.put("rejectionReason",
                            v.getRejectionReason() != null ? v.getRejectionReason() : "");
                    result.put("commissionRate", v.getCommissionRate());
                    return ResponseEntity.ok(result);
                })
                .orElse(ResponseEntity.notFound().build());
    }
}