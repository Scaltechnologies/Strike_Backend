package com.redemption_service.vendor.controller;

import com.redemption_service.common.dto.RedemptionRequest;
import com.redemption_service.common.dto.RedemptionResponse;
import com.redemption_service.common.response.ApiResponse;
import com.redemption_service.common.response.PageResponse;
import com.redemption_service.common.service.IdempotencyService;
import com.redemption_service.common.service.RedemptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/redemptions")
@RequiredArgsConstructor
public class VendorRedemptionController {

    private final RedemptionService redemptionService;
    private final IdempotencyService idempotencyService;

    @PostMapping
    @PreAuthorize("hasRole('VENDOR')")
    public ResponseEntity<?> redeem(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody RedemptionRequest request) {

        // 1. Return cached response or 409 if already in-flight
        Optional<ResponseEntity<?>> cached = idempotencyService.check(idempotencyKey);
        if (cached.isPresent()) return cached.get();

        // 2. Reserve key before processing to prevent concurrent duplicates
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            if (!idempotencyService.reserve(idempotencyKey)) {
                return ResponseEntity.status(HttpStatus.CONFLICT)
                        .body(Map.of("error",
                                "A request with this Idempotency-Key is already being processed. Retry in a moment."));
            }
        }

        // 3. Process — cancel reservation on failure so client can retry
        try {
            ApiResponse<RedemptionResponse> result = ApiResponse.success(
                    "Redemption processed", redemptionService.redeem(request));
            idempotencyService.complete(idempotencyKey, result, HttpStatus.CREATED.value());
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (RuntimeException e) {
            idempotencyService.cancel(idempotencyKey);
            throw e;
        }
    }

    @GetMapping("/store/{storeId}")
    @PreAuthorize("hasAnyRole('VENDOR', 'ADMIN')")
    public ApiResponse<PageResponse<RedemptionResponse>> getByStore(
            @PathVariable Long storeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(redemptionService.getByStore(storeId, page, size));
    }
}