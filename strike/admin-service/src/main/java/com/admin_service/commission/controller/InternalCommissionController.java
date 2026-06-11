package com.admin_service.commission.controller;

import com.admin_service.commission.dto.CommissionRecordResponse;
import com.admin_service.commission.dto.RecordCommissionRequest;
import com.admin_service.commission.service.CommissionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/admin/commission")
@RequiredArgsConstructor
public class InternalCommissionController {

    private final CommissionService commissionService;

    @PostMapping
    public ResponseEntity<CommissionRecordResponse> record(@Valid @RequestBody RecordCommissionRequest request) {
        try {
            return ResponseEntity.ok(commissionService.record(request));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}