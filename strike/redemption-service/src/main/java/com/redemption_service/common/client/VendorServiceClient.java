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
                Long categoryId = item.has("categoryId") && !item.get("categoryId").isNull()
                        ? item.get("categoryId").asLong() : null;
                String availabilityStatus = item.has("availabilityStatus") && !item.get("availabilityStatus").isNull()
                        ? item.get("availabilityStatus").asText() : null;
                result.put(id, new MenuItemInfo(id, name, price, itemStoreId, categoryId, availabilityStatus));
            }
        }
        return result;
    }

    /**
     * Returns true if the given vendor owns the given store.
     * Used before processing a redemption to prevent cross-vendor attacks.
     */
    public boolean verifyVendorOwnsStore(Long vendorId, Long storeId) {
        String url = vendorServiceUrl + "/internal/vendors/" + vendorId + "/owns-store/" + storeId;
        JsonNode response = restTemplate.getForObject(url, JsonNode.class);
        return response != null && response.get("owned").asBoolean(false);
    }

    public record MenuItemInfo(Long id, String name, BigDecimal price, Long storeId, Long categoryId,
                               String availabilityStatus) {}
}