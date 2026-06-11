package com.ledger_service.vendor.controller;

import com.ledger_service.common.dto.TransactionResponse;
import com.ledger_service.common.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/ledger")
@RequiredArgsConstructor
public class VendorTransactionController {

    private final TransactionService transactionService;

    @GetMapping("/store/{storeId}")
    @PreAuthorize("hasAnyRole('VENDOR', 'ADMIN')")
    public List<TransactionResponse> getByStore(@PathVariable Long storeId) {
        return transactionService.getByStore(storeId);
    }
}