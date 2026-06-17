package com.redemption_service.common.service;

import com.redemption_service.common.dto.RedemptionQueueResponse;
import com.redemption_service.common.dto.RedemptionRequest;
import com.redemption_service.common.dto.RedemptionResponse;
import com.redemption_service.common.response.PageResponse;

import java.util.List;

public interface RedemptionService {

    // ── Legacy: vendor-POS direct redemption (balance deducted immediately) ──
    RedemptionResponse redeem(Long vendorId, RedemptionRequest request);

    // ── Phase 5: user-initiated request flow ─────────────────────────────────
    RedemptionResponse requestRedemption(Long userId, RedemptionRequest request);
    RedemptionResponse approveRedemption(Long vendorId, Long redemptionId);
    RedemptionResponse rejectRedemption(Long vendorId, Long redemptionId, String reason);
    List<RedemptionQueueResponse> getPendingQueue(Long storeId);

    // ── Read ──────────────────────────────────────────────────────────────────
    RedemptionResponse getById(Long id);
    PageResponse<RedemptionResponse> getBySubscription(Long subscriptionId, int page, int size);
    PageResponse<RedemptionResponse> getByStore(Long storeId, int page, int size);
    PageResponse<RedemptionResponse> getByUser(Long userId, int page, int size);
    PageResponse<RedemptionResponse> getAll(int page, int size);
}