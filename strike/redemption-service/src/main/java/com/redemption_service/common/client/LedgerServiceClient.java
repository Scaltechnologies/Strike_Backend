package com.redemption_service.common.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class LedgerServiceClient {

    private final RestTemplate restTemplate;

    @Value("${services.ledger-url}")
    private String ledgerServiceUrl;

    public void recordRedemption(Long storeId, Long userId, Long subscriptionId, BigDecimal amount, String remarks) {
        String url = ledgerServiceUrl + "/internal/transactions";
        Map<String, Object> request = Map.of(
                "storeId", storeId,
                "customerId", userId,
                "subscriptionId", subscriptionId,
                "transactionType", "REDEMPTION",
                "amount", amount,
                "remarks", remarks
        );
        restTemplate.postForObject(url, request, Object.class);
    }
}