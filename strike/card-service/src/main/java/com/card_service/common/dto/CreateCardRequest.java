package com.card_service.common.dto;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

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
}