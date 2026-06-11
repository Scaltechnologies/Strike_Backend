package com.ledger_service.common.dto;

import com.ledger_service.common.enums.TransactionType;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class TransactionResponse {
    private Long id;
    private Long storeId;
    private Long customerId;
    private Long subscriptionId;
    private TransactionType transactionType;
    private BigDecimal amount;
    private String remarks;
    private LocalDateTime createdAt;
}