package com.vendor_service.common.client;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class CardServiceClient {

    private final RestTemplate restTemplate;

    @Value("${services.card-url}")
    private String cardServiceUrl;

    /**
     * Returns true if the category is currently mapped to at least one active card definition.
     * Throws RuntimeException if card-service is unreachable.
     */
    public boolean isCategoryMappedToActiveCard(Long categoryId) {
        String url = cardServiceUrl + "/internal/cards/category-mappings/active/" + categoryId;
        JsonNode response = restTemplate.getForObject(url, JsonNode.class);
        if (response == null) {
            throw new RuntimeException("No response from card-service when checking category mapping");
        }
        return response.get("mapped").asBoolean();
    }
}