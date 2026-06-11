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
import java.util.List;
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
        stats.put("totalUsers", countFromList(authServiceUrl + "/internal/users"));

        // Vendors by status
        Map<String, Object> vendors = new LinkedHashMap<>();
        vendors.put("total", vendorRecordRepository.count());
        vendors.put("pending", vendorRecordRepository.findByStatus("PENDING").size());
        vendors.put("active", vendorRecordRepository.findByStatus("ACTIVE").size());
        vendors.put("suspended", vendorRecordRepository.findByStatus("SUSPENDED").size());
        vendors.put("rejected", vendorRecordRepository.findByStatus("REJECTED").size());
        stats.put("vendors", vendors);

        // Transactions (uses admin endpoint — no auth header needed, internal path)
        Map<String, Object> transactions = new LinkedHashMap<>();
        List<Map<String, Object>> txList = fetchMapList(ledgerServiceUrl + "/api/admin/ledger/all");
        transactions.put("total", txList.size());
        transactions.put("totalRevenue", sumField(txList, "amount"));
        stats.put("transactions", transactions);

        // Redemptions (uses admin endpoint — no auth header needed, internal path)
        Map<String, Object> redemptions = new LinkedHashMap<>();
        Object redemptionData = fetchAdminData(redemptionServiceUrl + "/api/admin/redemptions/all");
        List<Map<String, Object>> redList = extractList(redemptionData);
        redemptions.put("total", redList.size());
        redemptions.put("totalAmount", sumField(redList, "totalAmount"));
        stats.put("redemptions", redemptions);

        return stats;
    }

    private int countFromList(String url) {
        try {
            Object data = restTemplate.getForObject(url, Object.class);
            if (data instanceof List<?> list) return list.size();
        } catch (Exception ignored) {}
        return 0;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> fetchMapList(String url) {
        try {
            Object data = restTemplate.getForObject(url, Object.class);
            if (data instanceof List<?> list) return (List<Map<String, Object>>) list;
        } catch (Exception ignored) {}
        return List.of();
    }

    private Object fetchAdminData(String url) {
        try {
            return restTemplate.getForObject(url, Object.class);
        } catch (Exception ignored) {}
        return null;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractList(Object data) {
        if (data instanceof List<?> list) return (List<Map<String, Object>>) list;
        if (data instanceof Map<?, ?> map) {
            Object inner = map.get("data");
            if (inner instanceof List<?> list) return (List<Map<String, Object>>) list;
        }
        return List.of();
    }

    private BigDecimal sumField(List<Map<String, Object>> list, String field) {
        BigDecimal sum = BigDecimal.ZERO;
        for (Map<String, Object> item : list) {
            Object val = item.get(field);
            if (val instanceof Number n) {
                sum = sum.add(new BigDecimal(n.toString()));
            }
        }
        return sum;
    }
}