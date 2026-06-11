package com.admin_service.commission.controller;

import com.admin_service.commission.dto.CommissionRecordResponse;
import com.admin_service.commission.service.CommissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/commissions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCommissionController {

    private final CommissionService commissionService;

    @GetMapping
    public List<CommissionRecordResponse> getAll() {
        return commissionService.getAll();
    }

    @GetMapping("/pending")
    public List<CommissionRecordResponse> getPending() {
        return commissionService.getPending();
    }

    @GetMapping("/vendor/{vendorId}")
    public List<CommissionRecordResponse> getByVendor(@PathVariable Long vendorId) {
        return commissionService.getByVendor(vendorId);
    }

    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        return commissionService.getStats();
    }

    @PatchMapping("/{id}/settle")
    public ResponseEntity<CommissionRecordResponse> settle(@PathVariable Long id) {
        return ResponseEntity.ok(commissionService.settle(id));
    }

    @PatchMapping("/vendor/{vendorId}/settle-all")
    public ResponseEntity<Map<String, Object>> settleByVendor(@PathVariable Long vendorId) {
        return ResponseEntity.ok(commissionService.settleByVendor(vendorId));
    }
}