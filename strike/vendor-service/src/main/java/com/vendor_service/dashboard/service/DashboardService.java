package com.vendor_service.dashboard.service;

import com.vendor_service.dashboard.dto.response.DashboardSummaryResponse;

public interface DashboardService {

    DashboardSummaryResponse getDashboardSummary(Long storeId);
}