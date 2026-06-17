package com.redemption_service.common.dto;

import com.redemption_service.common.enums.RedemptionStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class RedemptionQueueResponse {
    private Long id;
    private Long subscriptionId;
    private Long userId;
    private String customerName;
    private Long storeId;
    private BigDecimal totalAmount;
    private RedemptionStatus status;
    private String initiatedBy;
    private List<RedemptionItemResponse> items;
    private LocalDateTime createdAt;
}
