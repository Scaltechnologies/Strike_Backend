package com.card_service.common.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class SubscriptionRedemptionContext {
    private Long userId;
    private Long storeId;
    private Long cardDefinitionId;
    private String status;
    private List<Long> eligibleCategoryIds;
    private List<Long> eligibleMenuItemIds;
}