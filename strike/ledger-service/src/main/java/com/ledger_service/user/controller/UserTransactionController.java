package com.ledger_service.user.controller;

import com.ledger_service.common.dto.TransactionResponse;
import com.ledger_service.common.response.ApiResponse;
import com.ledger_service.common.response.PageResponse;
import com.ledger_service.common.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ledger")
@RequiredArgsConstructor
public class UserTransactionController {

    private final TransactionService transactionService;

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<PageResponse<TransactionResponse>> getByUser(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(transactionService.getByCustomer(userId, page, size));
    }

    @GetMapping("/subscription/{subscriptionId}")
    @PreAuthorize("hasAnyRole('USER', 'ADMIN')")
    public ApiResponse<PageResponse<TransactionResponse>> getBySubscription(
            @PathVariable Long subscriptionId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(transactionService.getBySubscription(subscriptionId, page, size));
    }
}