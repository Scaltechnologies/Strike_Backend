package com.card_service.common.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class EligibleMenuResponse {
    private Long subscriptionId;
    private String cardName;
    private List<EligibleCategory> categories;

    @Data
    @Builder
    public static class EligibleCategory {
        private Long categoryId;
        private String categoryName;
        private List<EligibleItem> items;
    }

    @Data
    @Builder
    public static class EligibleItem {
        private Long itemId;
        private String name;
        private BigDecimal price;
        private String itemType;
        private String availabilityStatus;
    }
}