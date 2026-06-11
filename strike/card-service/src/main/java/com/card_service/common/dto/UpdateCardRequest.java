package com.card_service.common.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class UpdateCardRequest {

    @Size(min = 2, max = 100, message = "Card name must be 2-100 characters")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @DecimalMin(value = "0.01", message = "Card price must be greater than zero")
    private BigDecimal cardPrice;

    @DecimalMin(value = "0.01", message = "Wallet amount must be greater than zero")
    private BigDecimal walletAmount;

    @Min(value = 1, message = "Validity must be at least 1 day")
    private Integer validityInDays;

    @Size(max = 500, message = "Image URL must not exceed 500 characters")
    private String imageUrl;

    private Boolean isActive;
}