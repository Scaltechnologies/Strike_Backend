package com.vendor_service.dashboard.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryResponse {

    private Long storeId;

    private BigDecimal totalRevenue;
    private BigDecimal totalRedemptionAmount;
    private Long totalCardsSold;

    private Long totalActiveSubscriptions;
    private Long totalSubscriptions;

    private Long totalRedemptions;

    private List<Object> recentTransactions;
    private List<Object> recentRedemptions;
    private List<Object> activeCards;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CardSalesStats {
        private Long cardId;
        private String cardName;
        private Long salesCount;
        private BigDecimal totalRevenue;
    }
}