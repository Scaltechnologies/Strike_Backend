package com.ledger_service.common.service.impl;

import com.ledger_service.common.dto.RecordTransactionRequest;
import com.ledger_service.common.dto.TransactionResponse;
import com.ledger_service.common.entity.Transaction;
import com.ledger_service.common.repository.TransactionRepository;
import com.ledger_service.common.response.PageResponse;
import com.ledger_service.common.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository transactionRepository;

    @Override
    public TransactionResponse record(RecordTransactionRequest request) {
        Transaction transaction = Transaction.builder()
                .storeId(request.getStoreId())
                .customerId(request.getCustomerId())
                .subscriptionId(request.getSubscriptionId())
                .transactionType(request.getTransactionType())
                .amount(request.getAmount())
                .remarks(request.getRemarks())
                .build();
        return toResponse(transactionRepository.save(transaction));
    }

    @Override
    public PageResponse<TransactionResponse> getByStore(Long storeId, int page, int size) {
        return PageResponse.from(
                transactionRepository.findByStoreIdOrderByCreatedAtDesc(storeId, PageRequest.of(page, size))
                        .map(this::toResponse));
    }

    @Override
    public PageResponse<TransactionResponse> getByCustomer(Long customerId, int page, int size) {
        return PageResponse.from(
                transactionRepository.findByCustomerIdOrderByCreatedAtDesc(customerId, PageRequest.of(page, size))
                        .map(this::toResponse));
    }

    @Override
    public PageResponse<TransactionResponse> getBySubscription(Long subscriptionId, int page, int size) {
        return PageResponse.from(
                transactionRepository.findBySubscriptionIdOrderByCreatedAtDesc(subscriptionId, PageRequest.of(page, size))
                        .map(this::toResponse));
    }

    @Override
    public PageResponse<TransactionResponse> getAll(int page, int size) {
        return PageResponse.from(
                transactionRepository.findAll(PageRequest.of(page, size))
                        .map(this::toResponse));
    }

    private TransactionResponse toResponse(Transaction t) {
        return TransactionResponse.builder()
                .id(t.getId())
                .storeId(t.getStoreId())
                .customerId(t.getCustomerId())
                .subscriptionId(t.getSubscriptionId())
                .transactionType(t.getTransactionType())
                .amount(t.getAmount())
                .remarks(t.getRemarks())
                .createdAt(t.getCreatedAt())
                .build();
    }
}