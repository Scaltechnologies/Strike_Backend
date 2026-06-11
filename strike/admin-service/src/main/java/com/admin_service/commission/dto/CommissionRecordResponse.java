package com.admin_service.commission.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class CommissionRecordResponse {
    private Long id;
    private Long vendorId;
    private String vendorName;
    private Long storeId;
    private Long subscriptionId;
    private Long userId;
    private BigDecimal subscriptionAmount;
    private BigDecimal commissionRate;
    private BigDecimal commissionAmount;
    private String status;
    private LocalDateTime settledAt;
    private LocalDateTime createdAt;
}