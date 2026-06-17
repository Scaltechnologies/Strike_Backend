package com.card_service.common.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class EligibleItemInfo {
    private Long id;
    private String name;
    private BigDecimal price;
}