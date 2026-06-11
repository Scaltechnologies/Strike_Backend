package com.redemption_service.common.service;

import com.redemption_service.common.dto.RedemptionRequest;
import com.redemption_service.common.dto.RedemptionResponse;
import com.redemption_service.common.response.PageResponse;

public interface RedemptionService {
    RedemptionResponse redeem(RedemptionRequest request);
    RedemptionResponse getById(Long id);
    PageResponse<RedemptionResponse> getBySubscription(Long subscriptionId, int page, int size);
    PageResponse<RedemptionResponse> getByStore(Long storeId, int page, int size);
    PageResponse<RedemptionResponse> getByUser(Long userId, int page, int size);
    PageResponse<RedemptionResponse> getAll(int page, int size);
}
