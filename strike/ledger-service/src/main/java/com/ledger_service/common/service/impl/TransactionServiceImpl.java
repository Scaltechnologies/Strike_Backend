package com.ledger_service.common.service.impl;

import com.ledger_service.common.dto.RecordTransactionRequest;
import com.ledger_service.common.dto.TransactionResponse;
import com.ledger_service.common.entity.Transaction;
import com.ledger_service.common.repository.TransactionRepository;
import com.ledger_service.common.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

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
    public List<TransactionResponse> getByStore(Long storeId) {
        return transactionRepository.findByStoreIdOrderByCreatedAtDesc(storeId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    public List<TransactionResponse> getByCustomer(Long customerId) {
        return transactionRepository.findByCustomerIdOrderByCreatedAtDesc(customerId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    public List<TransactionResponse> getBySubscription(Long subscriptionId) {
        return transactionRepository.findBySubscriptionIdOrderByCreatedAtDesc(subscriptionId)
                .stream().map(this::toResponse).toList();
    }

    @Override
    public List<TransactionResponse> getAll() {
        return transactionRepository.findAll().stream().map(this::toResponse).toList();
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