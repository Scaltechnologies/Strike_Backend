package com.vendor_service.dashboard.service.impl;

import com.vendor_service.dashboard.dto.response.DashboardSummaryResponse;
import com.vendor_service.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final RestTemplate restTemplate;

    @Value("${services.card-url}")
    private String cardServiceUrl;

    @Value("${services.redemption-url}")
    private String redemptionServiceUrl;

    @Value("${services.ledger-url}")
    private String ledgerServiceUrl;

    @Override
    public DashboardSummaryResponse getDashboardSummary(Long storeId) {
        List<Object> activeCards   = fetchList(cardServiceUrl + "/api/cards/store/" + storeId);
        List<Object> subscriptions = fetchList(cardServiceUrl + "/api/cards/subscriptions/store/" + storeId);
        List<Object> redemptions   = fetchList(redemptionServiceUrl + "/api/redemptions/store/" + storeId);
        List<Object> transactions  = fetchList(ledgerServiceUrl + "/api/ledger/store/" + storeId);

        long activeSubCount = subscriptions.stream()
                .filter(obj -> obj instanceof Map)
                .filter(obj -> "ACTIVE".equals(((Map<?, ?>) obj).get("status")))
                .count();

        BigDecimal totalRevenue = sumField(transactions, "amount");
        BigDecimal totalRedemptionAmount = sumField(redemptions, "totalAmount");

        return DashboardSummaryResponse.builder()
                .storeId(storeId)
                .totalRevenue(totalRevenue)
                .totalRedemptionAmount(totalRedemptionAmount)
                .totalCardsSold((long) subscriptions.size())
                .totalActiveSubscriptions(activeSubCount)
                .totalSubscriptions((long) subscriptions.size())
                .totalRedemptions((long) redemptions.size())
                .recentTransactions(transactions.stream().limit(10).toList())
                .recentRedemptions(redemptions.stream().limit(10).toList())
                .activeCards(activeCards)
                .build();
    }

    private BigDecimal sumField(List<Object> items, String field) {
        return items.stream()
                .filter(obj -> obj instanceof Map)
                .map(obj -> {
                    Object val = ((Map<?, ?>) obj).get(field);
                    if (val == null) return BigDecimal.ZERO;
                    try { return new BigDecimal(val.toString()); }
                    catch (NumberFormatException ignored) { return BigDecimal.ZERO; }
                })
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private List<Object> fetchList(String url) {
        try {
            Object response = restTemplate.getForObject(url, Object.class);
            if (response instanceof List<?> list) {
                return new ArrayList<>(list);
            }
            if (response instanceof Map<?, ?> map) {
                Object data = map.get("data");
                if (data instanceof List<?> dataList) return new ArrayList<>(dataList);
            }
        } catch (Exception ignored) {}
        return new ArrayList<>();
    }
}