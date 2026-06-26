package com.redemption_service.common.dto;

import com.redemption_service.common.enums.RedemptionStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class RedemptionResponse {
    private Long id;
    private Long subscriptionId;
    private Long userId;
    private Long storeId;
    private BigDecimal totalAmount;
    private BigDecimal remainingBalance;
    private RedemptionStatus status;
    private String initiatedBy;
    private String customerName;
    private List<RedemptionItemResponse> items;
    private LocalDateTime createdAt;
    private LocalDateTime approvedAt;
    private LocalDateTime rejectedAt;
    private String failureReason;
}