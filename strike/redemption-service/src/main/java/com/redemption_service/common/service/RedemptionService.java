package com.redemption_service.common.service;

import com.redemption_service.common.dto.RedemptionRequest;
import com.redemption_service.common.dto.RedemptionResponse;

import java.util.List;

public interface RedemptionService {
    RedemptionResponse redeem(RedemptionRequest request);
    RedemptionResponse getById(Long id);
    List<RedemptionResponse> getBySubscription(Long subscriptionId);
    List<RedemptionResponse> getByStore(Long storeId);
    List<RedemptionResponse> getByUser(Long userId);
    List<RedemptionResponse> getAll();
}