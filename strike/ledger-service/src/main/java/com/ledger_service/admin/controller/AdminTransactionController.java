package com.ledger_service.admin.controller;

import com.ledger_service.common.dto.TransactionResponse;
import com.ledger_service.common.repository.TransactionRepository;
import com.ledger_service.common.response.PageResponse;
import com.ledger_service.common.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/ledger")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminTransactionController {

    private final TransactionService transactionService;
    private final TransactionRepository transactionRepository;

    @GetMapping("/stats")
    public Map<String, Object> getStats() {
        BigDecimal totalRevenue = transactionRepository.sumAllAmounts();
        return Map.of(
                "totalTransactions", transactionRepository.count(),
                "totalRevenue", totalRevenue != null ? totalRevenue : BigDecimal.ZERO
        );
    }

    @GetMapping("/all")
    public PageResponse<TransactionResponse> getAll(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return transactionService.getAll(page, size);
    }

    @GetMapping("/subscription/{subscriptionId}")
    public PageResponse<TransactionResponse> getBySubscription(
            @PathVariable Long subscriptionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return transactionService.getBySubscription(subscriptionId, page, size);
    }

    @GetMapping("/store/{storeId}")
    public PageResponse<TransactionResponse> getByStore(
            @PathVariable Long storeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return transactionService.getByStore(storeId, page, size);
    }

    @GetMapping("/user/{userId}")
    public PageResponse<TransactionResponse> getByUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return transactionService.getByCustomer(userId, page, size);
    }
}