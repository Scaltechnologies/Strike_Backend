package com.redemption_service.common.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.redemption_service.common.dto.SubscriptionRedemptionContext;
import com.redemption_service.common.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
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

    public List<Long> getEligibleCategoryIds(Long subscriptionId) {
        String url = cardServiceUrl + "/internal/subscriptions/" + subscriptionId + "/eligible-category-ids";
        JsonNode response = restTemplate.getForObject(url, JsonNode.class);
        List<Long> ids = new ArrayList<>();
        if (response != null && response.isArray()) {
            for (JsonNode node : response) {
                ids.add(node.asLong());
            }
        }
        return ids;
    }

    /**
     * Single call that returns subscription owner, store, status, and eligible category IDs.
     * Used by RedemptionServiceImpl to validate context in one round-trip.
     */
    public SubscriptionRedemptionContext getRedemptionContext(Long subscriptionId) {
        String url = cardServiceUrl + "/internal/subscriptions/" + subscriptionId + "/redemption-context";
        JsonNode node = restTemplate.getForObject(url, JsonNode.class);
        if (node == null) {
            throw new BadRequestException("Unable to fetch subscription context. Please try again.");
        }
        SubscriptionRedemptionContext ctx = new SubscriptionRedemptionContext();
        ctx.setUserId(node.get("userId").asLong());
        ctx.setStoreId(node.get("storeId").asLong());
        ctx.setCardDefinitionId(node.get("cardDefinitionId").asLong());
        ctx.setStatus(node.get("status").asText());
        List<Long> categoryIds = new ArrayList<>();
        JsonNode catNode = node.get("eligibleCategoryIds");
        if (catNode != null && catNode.isArray()) {
            for (JsonNode id : catNode) categoryIds.add(id.asLong());
        }
        ctx.setEligibleCategoryIds(categoryIds);

        List<Long> itemIds = new ArrayList<>();
        JsonNode itemNode = node.get("eligibleMenuItemIds");
        if (itemNode != null && itemNode.isArray()) {
            for (JsonNode id : itemNode) itemIds.add(id.asLong());
        }
        ctx.setEligibleMenuItemIds(itemIds);

        return ctx;
    }
}