package com.redemption_service.vendor.controller;

import com.redemption_service.common.dto.RedemptionRequest;
import com.redemption_service.common.dto.RedemptionResponse;
import com.redemption_service.common.response.ApiResponse;
import com.redemption_service.common.service.RedemptionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/redemptions")
@RequiredArgsConstructor
public class VendorRedemptionController {

    private final RedemptionService redemptionService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('VENDOR')")
    public ApiResponse<RedemptionResponse> redeem(@Valid @RequestBody RedemptionRequest request) {
        return ApiResponse.success("Redemption processed", redemptionService.redeem(request));
    }

    @GetMapping("/store/{storeId}")
    @PreAuthorize("hasAnyRole('VENDOR', 'ADMIN')")
    public ApiResponse<List<RedemptionResponse>> getByStore(@PathVariable Long storeId) {
        return ApiResponse.success(redemptionService.getByStore(storeId));
    }
}