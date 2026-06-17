package com.card_service.common.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class CardDefinitionResponse {
    private Long id;
    private Long vendorId;
    private Long storeId;
    private String name;
    private String description;
    private BigDecimal cardPrice;
    private BigDecimal walletAmount;
    private BigDecimal bonusAmount;
    private Integer validityInDays;
    private String imageUrl;
    private Boolean isActive;
    private List<Long> categoryIds;
    /** IDs only — present in all responses. */
    private List<Long> eligibleMenuItemIds;
    /** Rich item info (id + name + price) — populated only in single-card-get and create/update responses. */
    private List<EligibleItemInfo> eligibleItems;
    private LocalDateTime createdAt;
}