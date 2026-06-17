package com.redemption_service.common.dto;

import lombok.Data;

import java.util.List;

@Data
public class SubscriptionRedemptionContext {
    private Long userId;
    private Long storeId;
    private Long cardDefinitionId;
    private String status;
    private List<Long> eligibleCategoryIds;
    private List<Long> eligibleMenuItemIds;
}