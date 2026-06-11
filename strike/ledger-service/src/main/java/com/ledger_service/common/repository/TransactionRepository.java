package com.ledger_service.common.repository;

import com.ledger_service.common.entity.Transaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.math.BigDecimal;

public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    Page<Transaction> findByStoreIdOrderByCreatedAtDesc(Long storeId, Pageable pageable);
    Page<Transaction> findByCustomerIdOrderByCreatedAtDesc(Long customerId, Pageable pageable);
    Page<Transaction> findBySubscriptionIdOrderByCreatedAtDesc(Long subscriptionId, Pageable pageable);

    @Query("SELECT COALESCE(SUM(t.amount), 0) FROM Transaction t")
    BigDecimal sumAllAmounts();
}
