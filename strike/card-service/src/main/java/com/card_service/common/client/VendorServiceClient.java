package com.card_service.common.client;

import com.card_service.common.dto.EligibleItemInfo;
import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class VendorServiceClient {

    private final RestTemplate restTemplate;

    @Value("${services.vendor-url}")
    private String vendorServiceUrl;

    public String getVendorServiceUrl() { return vendorServiceUrl; }

    /**
     * Returns the subset of the provided categoryIds that are valid (ACTIVE, belong to the store).
     */
    public List<Long> validateCategoryIds(List<Long> categoryIds, Long storeId) {
        String ids = categoryIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        String url = vendorServiceUrl + "/internal/categories/validate?categoryIds=" + ids + "&storeId=" + storeId;
        JsonNode response = restTemplate.getForObject(url, JsonNode.class);
        List<Long> valid = new ArrayList<>();
        if (response != null && response.isArray()) {
            for (JsonNode node : response) {
                valid.add(node.asLong());
            }
        }
        return valid;
    }

    /**
     * Fetches ACTIVE categories (with their menu items) for the given IDs.
     * Used for card preview and the user-facing eligible menu.
     */
    public List<CategoryInfo> getCategoriesWithItems(List<Long> categoryIds) {
        if (categoryIds.isEmpty()) return List.of();
        String ids = categoryIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        String url = vendorServiceUrl + "/internal/categories/by-ids?ids=" + ids;
        JsonNode response = restTemplate.getForObject(url, JsonNode.class);
        List<CategoryInfo> result = new ArrayList<>();
        if (response != null && response.isArray()) {
            for (JsonNode cat : response) {
                List<ItemInfo> items = new ArrayList<>();
                JsonNode itemsNode = cat.get("items");
                if (itemsNode != null && itemsNode.isArray()) {
                    for (JsonNode item : itemsNode) {
                        String availabilityStatus = item.has("availabilityStatus") && !item.get("availabilityStatus").isNull()
                                ? item.get("availabilityStatus").asText() : null;
                        items.add(new ItemInfo(
                                item.get("id").asLong(),
                                item.get("name").asText(),
                                new BigDecimal(item.get("price").asText()),
                                item.get("itemType").asText(),
                                availabilityStatus
                        ));
                    }
                }
                result.add(new CategoryInfo(
                        cat.get("id").asLong(),
                        cat.get("name").asText(),
                        items
                ));
            }
        }
        return result;
    }

    /**
     * Returns the subset of menuItemIds that are valid — belonging to the given store
     * and, when categoryIds is non-empty, also belonging to one of those categories.
     * Used to validate eligibleMenuItemIds during card create/update.
     */
    public List<Long> validateMenuItemIds(List<Long> menuItemIds, Long storeId, List<Long> categoryIds) {
        String ids = menuItemIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        StringBuilder url = new StringBuilder(vendorServiceUrl)
                .append("/internal/menu-items/validate?menuItemIds=").append(ids)
                .append("&storeId=").append(storeId);
        if (categoryIds != null && !categoryIds.isEmpty()) {
            String catIds = categoryIds.stream().map(String::valueOf).collect(Collectors.joining(","));
            url.append("&categoryIds=").append(catIds);
        }
        JsonNode response = restTemplate.getForObject(url.toString(), JsonNode.class);
        List<Long> valid = new ArrayList<>();
        if (response != null && response.isArray()) {
            for (JsonNode node : response) {
                valid.add(node.asLong());
            }
        }
        return valid;
    }

    /**
     * Returns EligibleItemInfo (id, name, price) for each of the given menu item IDs.
     * Used to enrich card detail responses with human-readable item names.
     */
    public List<EligibleItemInfo> getMenuItemsByIds(List<Long> menuItemIds) {
        if (menuItemIds.isEmpty()) return List.of();
        String ids = menuItemIds.stream().map(String::valueOf).collect(Collectors.joining(","));
        String url = vendorServiceUrl + "/internal/menu-items/by-ids?ids=" + ids;
        JsonNode response = restTemplate.getForObject(url, JsonNode.class);
        List<EligibleItemInfo> result = new ArrayList<>();
        if (response != null && response.isArray()) {
            for (JsonNode item : response) {
                result.add(EligibleItemInfo.builder()
                        .id(item.get("id").asLong())
                        .name(item.get("name").asText())
                        .price(new BigDecimal(item.get("price").asText()))
                        .build());
            }
        }
        return result;
    }

    public record CategoryInfo(Long id, String name, List<ItemInfo> items) {}
    public record ItemInfo(Long id, String name, BigDecimal price, String itemType, String availabilityStatus) {}
}