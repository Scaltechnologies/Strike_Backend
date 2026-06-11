package com.ledger_service.common.service;

import com.ledger_service.common.dto.RecordTransactionRequest;
import com.ledger_service.common.dto.TransactionResponse;
import com.ledger_service.common.response.PageResponse;

public interface TransactionService {
    TransactionResponse record(RecordTransactionRequest request);
    PageResponse<TransactionResponse> getByStore(Long storeId, int page, int size);
    PageResponse<TransactionResponse> getByCustomer(Long customerId, int page, int size);
    PageResponse<TransactionResponse> getBySubscription(Long subscriptionId, int page, int size);
    PageResponse<TransactionResponse> getAll(int page, int size);
}
