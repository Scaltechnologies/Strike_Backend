package com.ledger_service.internal.controller;

import com.ledger_service.common.dto.RecordTransactionRequest;
import com.ledger_service.common.dto.TransactionResponse;
import com.ledger_service.common.service.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/transactions")
@RequiredArgsConstructor
public class InternalTransactionController {

    private final TransactionService transactionService;

    @PostMapping
    public TransactionResponse record(@Valid @RequestBody RecordTransactionRequest request) {
        return transactionService.record(request);
    }
}