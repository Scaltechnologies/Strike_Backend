package com.ledger_service.common.repository;

import com.ledger_service.common.entity.Transaction;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByStoreIdOrderByCreatedAtDesc(Long storeId);
    List<Transaction> findByCustomerIdOrderByCreatedAtDesc(Long customerId);
    List<Transaction> findBySubscriptionIdOrderByCreatedAtDesc(Long subscriptionId);
}