package com.ledger_service.vendor.controller;

import com.ledger_service.common.dto.TransactionResponse;
import com.ledger_service.common.response.PageResponse;
import com.ledger_service.common.service.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ledger")
@RequiredArgsConstructor
public class VendorTransactionController {

    private final TransactionService transactionService;

    @GetMapping("/store/{storeId}")
    @PreAuthorize("hasAnyRole('VENDOR', 'ADMIN')")
    public PageResponse<TransactionResponse> getByStore(
            @PathVariable Long storeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return transactionService.getByStore(storeId, page, size);
    }
}