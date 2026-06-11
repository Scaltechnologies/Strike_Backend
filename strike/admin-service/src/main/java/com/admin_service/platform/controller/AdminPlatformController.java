package com.admin_service.platform.controller;

import com.admin_service.vendor.repository.VendorRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/platform")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminPlatformController {

    private final RestTemplate restTemplate;
    private final VendorRecordRepository vendorRecordRepository;

    @Value("${services.auth-url}")
    private String authServiceUrl;

    @Value("${services.redemption-url}")
    private String redemptionServiceUrl;

    @Value("${services.ledger-url}")
    private String ledgerServiceUrl;

    @GetMapping("/stats")
    public Map<String, Object> getPlatformStats() {
        Map<String, Object> stats = new LinkedHashMap<>();

        // Users
        stats.put("totalUsers", safeLong(authServiceUrl + "/internal/users/count"));

        // Vendors by status
        Map<String, Object> vendors = new LinkedHashMap<>();
        vendors.put("total",     vendorRecordRepository.count());
        vendors.put("pending",   vendorRecordRepository.countByStatus("PENDING"));
        vendors.put("active",    vendorRecordRepository.countByStatus("ACTIVE"));
        vendors.put("suspended", vendorRecordRepository.countByStatus("SUSPENDED"));
        vendors.put("rejected",  vendorRecordRepository.countByStatus("REJECTED"));
        stats.put("vendors", vendors);

        // Transactions — dedicated stats endpoint
        Map<String, Object> txStats = fetchStats(ledgerServiceUrl + "/api/admin/ledger/stats");
        stats.put("transactions", txStats);

        // Redemptions — dedicated stats endpoint
        Map<String, Object> redStats = fetchStats(redemptionServiceUrl + "/api/admin/redemptions/stats");
        stats.put("redemptions", redStats);

        return stats;
    }

    private long safeLong(String url) {
        try {
            Object result = restTemplate.getForObject(url, Object.class);
            if (result instanceof Number n) return n.longValue();
        } catch (Exception ignored) {}
        return 0L;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> fetchStats(String url) {
        try {
            Object result = restTemplate.getForObject(url, Object.class);
            if (result instanceof Map<?, ?> map) return (Map<String, Object>) map;
        } catch (Exception ignored) {}
        return Map.of("error", "unavailable");
    }
}
