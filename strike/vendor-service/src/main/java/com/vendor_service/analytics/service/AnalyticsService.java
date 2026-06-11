package com.vendor_service.analytics.service;

import com.vendor_service.analytics.dto.response.AnalyticsResponse;

public interface AnalyticsService {
    AnalyticsResponse getStoreAnalytics(Long storeId);
}