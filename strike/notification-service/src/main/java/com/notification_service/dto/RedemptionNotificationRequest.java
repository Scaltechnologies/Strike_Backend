package com.notification_service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RedemptionNotificationRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    private String storeName;

    @NotNull(message = "Total amount is required")
    private BigDecimal totalAmount;

    @NotNull(message = "Remaining balance is required")
    private BigDecimal remainingBalance;
}