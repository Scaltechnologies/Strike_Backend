package com.card_service.common.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class CreateCardRequest {

    @NotNull(message = "Store ID is required")
    private Long storeId;

    @NotBlank(message = "Card name is required")
    private String name;

    private String description;

    @NotNull @DecimalMin("0.01")
    private BigDecimal cardPrice;

    @NotNull @DecimalMin("0.01")
    private BigDecimal walletAmount;

    @NotNull @Min(1)
    private Integer validityInDays;

    private String imageUrl;

    @NotNull(message = "At least one menu category must be selected")
    @Size(min = 1, message = "At least one menu category must be selected")
    private List<Long> categoryIds;

    @Size(min = 1, message = "If provided, eligibleMenuItemIds must contain at least one item")
    private List<Long> eligibleMenuItemIds;
}