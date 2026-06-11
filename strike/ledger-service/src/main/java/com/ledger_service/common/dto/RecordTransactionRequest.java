package com.ledger_service.common.dto;

import com.ledger_service.common.enums.TransactionType;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class RecordTransactionRequest {

    @NotNull private Long storeId;
    @NotNull private Long customerId;
    @NotNull private Long subscriptionId;
    @NotNull private TransactionType transactionType;
    @NotNull private BigDecimal amount;
    private String remarks;
}