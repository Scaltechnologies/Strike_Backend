package com.redemption_service.common.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class RedemptionItemRequest {

    @NotNull(message = "Menu item ID is required")
    private Long menuItemId;

    @NotNull @Min(1)
    private Integer quantity;
}