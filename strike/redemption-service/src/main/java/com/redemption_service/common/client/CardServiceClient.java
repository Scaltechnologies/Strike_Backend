package com.redemption_service.common.client;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class CardServiceClient {

    private final RestTemplate restTemplate;

    @Value("${services.card-url}")
    private String cardServiceUrl;

    public BigDecimal getBalance(Long subscriptionId) {
        String url = cardServiceUrl + "/internal/subscriptions/" + subscriptionId + "/balance";
        JsonNode response = restTemplate.getForObject(url, JsonNode.class);
        return response != null ? new BigDecimal(response.get("walletBalance").asText()) : BigDecimal.ZERO;
    }

    public BigDecimal deductBalance(Long subscriptionId, BigDecimal amount) {
        String url = cardServiceUrl + "/internal/subscriptions/" + subscriptionId + "/deduct";
        JsonNode response = restTemplate.postForObject(url, Map.of("amount", amount), JsonNode.class);
        return response != null ? new BigDecimal(response.get("walletBalance").asText()) : BigDecimal.ZERO;
    }
}