package com.ledger_service.admin.controller;

import com.ledger_service.common.dto.TransactionResponse;
import com.ledger_service.common.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin/ledger")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminTransactionController {

    private final TransactionService transactionService;

    @GetMapping("/all")
    public List<TransactionResponse> getAll() {
        return transactionService.getAll();
    }

    @GetMapping("/subscription/{subscriptionId}")
    public List<TransactionResponse> getBySubscription(@PathVariable Long subscriptionId) {
        return transactionService.getBySubscription(subscriptionId);
    }

    @GetMapping("/store/{storeId}")
    public List<TransactionResponse> getByStore(@PathVariable Long storeId) {
        return transactionService.getByStore(storeId);
    }

    @GetMapping("/user/{userId}")
    public List<TransactionResponse> getByUser(@PathVariable Long userId) {
        return transactionService.getByCustomer(userId);
    }
}