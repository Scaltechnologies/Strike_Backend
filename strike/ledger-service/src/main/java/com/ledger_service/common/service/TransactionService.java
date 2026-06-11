package com.ledger_service.common.service;

import com.ledger_service.common.dto.RecordTransactionRequest;
import com.ledger_service.common.dto.TransactionResponse;

import java.util.List;

public interface TransactionService {
    TransactionResponse record(RecordTransactionRequest request);
    List<TransactionResponse> getByStore(Long storeId);
    List<TransactionResponse> getByCustomer(Long customerId);
    List<TransactionResponse> getBySubscription(Long subscriptionId);
    List<TransactionResponse> getAll();
}