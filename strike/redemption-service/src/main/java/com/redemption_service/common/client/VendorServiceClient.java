package com.redemption_service.common.client;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class VendorServiceClient {

    private final RestTemplate restTemplate;

    @Value("${services.vendor-url}")
    private String vendorServiceUrl;

    public Map<Long, MenuItemInfo> getMenuItems(Long storeId) {
        String url = vendorServiceUrl + "/internal/menu-items/store/" + storeId;
        JsonNode response = restTemplate.getForObject(url, JsonNode.class);
        Map<Long, MenuItemInfo> result = new HashMap<>();
        if (response != null && response.isArray()) {
            for (JsonNode item : response) {
                Long id = item.get("id").asLong();
                String name = item.get("name").asText();
                BigDecimal price = new BigDecimal(item.get("price").asText());
                Long itemStoreId = item.get("storeId").asLong();
                result.put(id, new MenuItemInfo(id, name, price, itemStoreId));
            }
        }
        return result;
    }

    public record MenuItemInfo(Long id, String name, BigDecimal price, Long storeId) {}
}