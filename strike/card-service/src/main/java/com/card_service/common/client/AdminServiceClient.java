package com.card_service.common.client;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AdminServiceClient {

    private final RestTemplate restTemplate;

    @Value("${services.admin-url}")
    private String adminServiceUrl;

    public void recordCommission(Long vendorId, Long storeId, Long subscriptionId,
                                 Long userId, BigDecimal subscriptionAmount) {
        String url = adminServiceUrl + "/internal/admin/commission";
        Map<String, Object> request = Map.of(
                "vendorId", vendorId,
                "storeId", storeId,
                "subscriptionId", subscriptionId,
                "userId", userId,
                "subscriptionAmount", subscriptionAmount
        );
        try {
            restTemplate.postForObject(url, request, Object.class);
        } catch (Exception ignored) {}
    }
}