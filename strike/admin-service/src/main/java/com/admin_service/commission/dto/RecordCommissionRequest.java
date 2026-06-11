package com.admin_service.commission.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RecordCommissionRequest {

    @NotNull(message = "Vendor ID is required")
    private Long vendorId;

    @NotNull(message = "Store ID is required")
    private Long storeId;

    @NotNull(message = "Subscription ID is required")
    private Long subscriptionId;

    @NotNull(message = "User ID is required")
    private Long userId;

    @NotNull(message = "Subscription amount is required")
    @DecimalMin(value = "0.01", message = "Subscription amount must be greater than zero")
    private BigDecimal subscriptionAmount;
}