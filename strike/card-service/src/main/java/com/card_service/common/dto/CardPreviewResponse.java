package com.card_service.common.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
public class CardPreviewResponse {
    private Long cardId;
    private String cardName;
    private String description;
    private BigDecimal cardPrice;
    private BigDecimal walletAmount;
    private Integer validityInDays;
    private List<MenuCategoryPreview> eligibleMenus;

    @Data
    @Builder
    public static class MenuCategoryPreview {
        private Long categoryId;
        private String categoryName;
        private List<MenuItemPreview> items;
    }

    @Data
    @Builder
    public static class MenuItemPreview {
        private Long itemId;
        private String name;
        private BigDecimal price;
        private String itemType;
        private String availabilityStatus;
    }
}