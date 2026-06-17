package com.redemption_service.vendor.controller;

import com.redemption_service.common.dto.RedemptionQueueResponse;
import com.redemption_service.common.dto.RedemptionRequest;
import com.redemption_service.common.dto.RedemptionResponse;
import com.redemption_service.common.dto.RejectRedemptionRequest;
import com.redemption_service.common.response.ApiResponse;
import com.redemption_service.common.response.PageResponse;
import com.redemption_service.common.service.IdempotencyService;
import com.redemption_service.common.service.RedemptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/redemptions")
@RequiredArgsConstructor
public class VendorRedemptionController {

    private final RedemptionService redemptionService;
    private final IdempotencyService idempotencyService;

    // ── Legacy POS-direct redemption ──────────────────────────────────────────

    @PostMapping
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<?> redeem(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody RedemptionRequest request) {

        Optional<ResponseEntity<?>> cached = idempotencyService.check(idempotencyKey);
        if (cached.isPresent()) return cached.get();

        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            if (!idempotencyService.reserve(idempotencyKey)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error",
                                "A request with this Idempotency-Key is already being processed."));
            }
        }

        Long vendorId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        try {
            ApiResponse<RedemptionResponse> result = ApiResponse.success(
                    "Redemption processed", redemptionService.redeem(vendorId, request));
            idempotencyService.complete(idempotencyKey, result, HttpStatus.CREATED.value());
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (RuntimeException e) {
            idempotencyService.cancel(idempotencyKey);
            throw e;
        }
    }

    // ── Phase 5: pending queue ────────────────────────────────────────────────

    @GetMapping("/store/{storeId}/queue")
    @PreAuthorize("hasRole('VENDOR')")
    public ApiResponse<List<RedemptionQueueResponse>> getPendingQueue(@PathVariable Long storeId) {
        return ApiResponse.success(redemptionService.getPendingQueue(storeId));
    }

    // ── Phase 5: approve ─────────────────────────────────────────────────────

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasRole('VENDOR')")
    public ApiResponse<RedemptionResponse> approve(@PathVariable Long id) {
        Long vendorId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        return ApiResponse.success("Redemption approved",
                redemptionService.approveRedemption(vendorId, id));
    }

    // ── Phase 5: reject ──────────────────────────────────────────────────────

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasRole('VENDOR')")
    public ApiResponse<RedemptionResponse> reject(
            @PathVariable Long id,
            @RequestBody(required = false) RejectRedemptionRequest body) {
        Long vendorId = (Long) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        String reason = body != null ? body.getReason() : null;
        return ApiResponse.success("Redemption rejected",
                redemptionService.rejectRedemption(vendorId, id, reason));
    }

    // ── History ───────────────────────────────────────────────────────────────

    @GetMapping("/store/{storeId}")
    @PreAuthorize("hasAnyRole('VENDOR', 'ADMIN')")
    public ApiResponse<PageResponse<RedemptionResponse>> getByStore(
            @PathVariable Long storeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(redemptionService.getByStore(storeId, page, size));
    }
}
