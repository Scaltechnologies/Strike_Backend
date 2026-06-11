package com.admin_service.commission.controller;

import com.admin_service.commission.dto.CommissionRecordResponse;
import com.admin_service.commission.service.CommissionService;
import com.admin_service.common.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/commissions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminCommissionController {

    private final CommissionService commissionService;

    @GetMapping
    public PageResponse<CommissionRecordResponse> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return commissionService.getAll(page, size);
    }

    @GetMapping("/pending")
    public PageResponse<CommissionRecordResponse> getPending(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return commissionService.getPending(page, size);
    }

    @GetMapping("/vendor/{vendorId}")
    public PageResponse<CommissionRecordResponse> getByVendor(
            @PathVariable Long vendorId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return commissionService.getByVendor(vendorId, page, size);
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
