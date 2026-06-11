package com.card_service.common.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PurchaseSubscriptionRequest {

    @NotNull(message = "Card ID is required")
    private Long cardDefinitionId;

    @NotNull(message = "Store ID is required")
    private Long storeId;
}