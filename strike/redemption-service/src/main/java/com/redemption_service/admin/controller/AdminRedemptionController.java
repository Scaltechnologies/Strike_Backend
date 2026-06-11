package com.redemption_service.admin.controller;

import com.redemption_service.common.dto.RedemptionResponse;
import com.redemption_service.common.repository.RedemptionRepository;
import com.redemption_service.common.response.ApiResponse;
import com.redemption_service.common.response.PageResponse;
import com.redemption_service.common.service.RedemptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/redemptions")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminRedemptionController {

    private final RedemptionService redemptionService;
    private final RedemptionRepository redemptionRepository;

    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        BigDecimal totalAmount = redemptionRepository.sumAllTotalAmounts();
        return Map.of(
                "totalRedemptions", redemptionRepository.count(),
                "totalAmount", totalAmount != null ? totalAmount : BigDecimal.ZERO
        );
    }

    @GetMapping("/all")
    public ApiResponse<PageResponse<RedemptionResponse>> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(redemptionService.getAll(page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<RedemptionResponse> getById(@PathVariable Long id) {
        return ApiResponse.success(redemptionService.getById(id));
    }

    @GetMapping("/store/{storeId}")
    public ApiResponse<PageResponse<RedemptionResponse>> getByStore(
            @PathVariable Long storeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(redemptionService.getByStore(storeId, page, size));
    }

    @GetMapping("/user/{userId}")
    public ApiResponse<PageResponse<RedemptionResponse>> getByUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(redemptionService.getByUser(userId, page, size));
    }

    @GetMapping("/subscription/{subscriptionId}")
    public ApiResponse<PageResponse<RedemptionResponse>> getBySubscription(
            @PathVariable Long subscriptionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(redemptionService.getBySubscription(subscriptionId, page, size));
    }
}