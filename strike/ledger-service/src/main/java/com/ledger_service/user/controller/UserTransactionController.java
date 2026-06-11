package com.ledger_service.user.controller;

import com.ledger_service.common.dto.TransactionResponse;
import com.ledger_service.common.response.ApiResponse;
import com.ledger_service.common.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ledger")
@RequiredArgsConstructor
public class UserTransactionController {

    private final TransactionService transactionService;

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<List<TransactionResponse>> getByUser(@PathVariable Long userId) {
        return ApiResponse.success(transactionService.getByCustomer(userId));
    }

    @GetMapping("/subscription/{subscriptionId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<List<TransactionResponse>> getBySubscription(@PathVariable Long subscriptionId) {
        return ApiResponse.success(transactionService.getBySubscription(subscriptionId));
    }
}