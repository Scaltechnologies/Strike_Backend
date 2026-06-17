package com.redemption_service.common.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class RedemptionRequest {

    @NotNull(message = "Subscription ID is required")
    private Long subscriptionId;

    @NotNull(message = "Store ID is required")
    private Long storeId;

    @Valid
    @NotEmpty(message = "At least one item is required")
    private List<RedemptionItemRequest> items;
}