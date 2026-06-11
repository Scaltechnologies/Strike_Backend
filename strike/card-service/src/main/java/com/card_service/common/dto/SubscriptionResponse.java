package com.card_service.common.dto;

import com.card_service.common.enums.SubscriptionStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class SubscriptionResponse {
    private Long id;
    private Long userId;
    private Long cardDefinitionId;
    private String cardName;
    private Long storeId;
    private BigDecimal walletBalance;
    private SubscriptionStatus status;
    private LocalDateTime purchasedAt;
    private LocalDateTime expiresAt;
    private LocalDateTime createdAt;
}