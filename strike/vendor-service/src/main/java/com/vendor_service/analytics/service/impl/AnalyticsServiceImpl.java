package com.vendor_service.analytics.service.impl;

import com.vendor_service.analytics.dto.response.AnalyticsResponse;
import com.vendor_service.analytics.service.AnalyticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AnalyticsServiceImpl implements AnalyticsService {

    private final RestTemplate restTemplate;

    @Value("${services.redemption-url}")
    private String redemptionServiceUrl;

    @Value("${services.ledger-url}")
    private String ledgerServiceUrl;

    @Override
    public AnalyticsResponse getStoreAnalytics(Long storeId) {
        List<Object> transactions = fetchList(ledgerServiceUrl + "/api/ledger/store/" + storeId);
        List<Object> redemptions = fetchList(redemptionServiceUrl + "/api/redemptions/store/" + storeId);

        return AnalyticsResponse.builder()
                .storeId(storeId)
                .transactions(transactions)
                .redemptions(redemptions)
                .build();
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