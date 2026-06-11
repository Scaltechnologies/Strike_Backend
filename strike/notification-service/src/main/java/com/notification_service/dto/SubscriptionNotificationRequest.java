package com.notification_service.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class SubscriptionNotificationRequest {

    @NotNull(message = "User ID is required")
    private Long userId;

    private String cardName;

    private String storeName;

    @NotNull(message = "Wallet balance is required")
    private BigDecimal walletBalance;

    @NotNull(message = "Expiry date is required")
    private LocalDateTime expiresAt;
}