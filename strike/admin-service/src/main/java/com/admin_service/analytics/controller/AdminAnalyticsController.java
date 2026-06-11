package com.admin_service.analytics.controller;

import com.admin_service.commission.entity.CommissionRecord;
import com.admin_service.commission.repository.CommissionRecordRepository;
import com.admin_service.vendor.entity.VendorRecord;
import com.admin_service.vendor.repository.VendorRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin/analytics")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminAnalyticsController {

    private final VendorRecordRepository vendorRecordRepository;
    private final CommissionRecordRepository commissionRecordRepository;
    private final RestTemplate restTemplate;

    @Value("${services.auth-url}")
    private String authServiceUrl;

    @Value("${services.ledger-url}")
    private String ledgerServiceUrl;

    @Value("${services.redemption-url}")
    private String redemptionServiceUrl;

    @Value("${services.vendor-url}")
    private String vendorServiceUrl;

    // ── Overview ─────────────────────────────────────────────────────────────

    @GetMapping("/overview")
    public Map<String, Object> overview() {
        Map<String, Object> result = new LinkedHashMap<>();

        result.put("totalUsers", safeCount(authServiceUrl + "/internal/users"));

        Map<String, Object> vendorBreakdown = new LinkedHashMap<>();
        vendorBreakdown.put("total", vendorRecordRepository.count());
        vendorBreakdown.put("active", vendorRecordRepository.findByStatus("ACTIVE").size());
        vendorBreakdown.put("pending", vendorRecordRepository.findByStatus("PENDING").size());
        vendorBreakdown.put("suspended", vendorRecordRepository.findByStatus("SUSPENDED").size());
        result.put("vendors", vendorBreakdown);

        result.put("totalSubscriptions", commissionRecordRepository.count());
        result.put("totalSubscriptionRevenue", commissionRecordRepository.totalSubscriptionRevenue());
        result.put("totalCommissionEarned",
                commissionRecordRepository.sumCommissionByStatus("PENDING")
                        .add(commissionRecordRepository.sumCommissionByStatus("SETTLED")));
        result.put("pendingCommission", commissionRecordRepository.sumCommissionByStatus("PENDING"));
        result.put("totalRedemptions", safeCount(redemptionServiceUrl + "/api/admin/redemptions/all"));

        return result;
    }

    // ── Revenue by Month ─────────────────────────────────────────────────────

    @GetMapping("/revenue")
    public Map<String, Object> revenueAnalytics() {
        List<CommissionRecord> records = commissionRecordRepository.findAll();

        Map<String, BigDecimal> revenueByMonth = new TreeMap<>();
        Map<String, BigDecimal> commissionByMonth = new TreeMap<>();

        for (CommissionRecord r : records) {
            if (r.getCreatedAt() == null) continue;
            String month = YearMonth.from(r.getCreatedAt()).toString();
            revenueByMonth.merge(month, r.getSubscriptionAmount(), BigDecimal::add);
            commissionByMonth.merge(month, r.getCommissionAmount(), BigDecimal::add);
        }

        List<Map<String, Object>> monthly = new ArrayList<>();
        for (String month : revenueByMonth.keySet()) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("month", month);
            entry.put("subscriptionRevenue", revenueByMonth.get(month));
            entry.put("commission", commissionByMonth.getOrDefault(month, BigDecimal.ZERO));
            monthly.add(entry);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("monthly", monthly);
        result.put("totalRevenue", commissionRecordRepository.totalSubscriptionRevenue());
        result.put("totalCommission",
                commissionRecordRepository.sumCommissionByStatus("PENDING")
                        .add(commissionRecordRepository.sumCommissionByStatus("SETTLED")));
        return result;
    }

    // ── Vendor Performance ───────────────────────────────────────────────────

    @GetMapping("/vendor-performance")
    public List<Map<String, Object>> vendorPerformance() {
        List<VendorRecord> vendors = vendorRecordRepository.findByStatus("ACTIVE");
        List<Map<String, Object>> result = new ArrayList<>();

        for (VendorRecord vendor : vendors) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("vendorId", vendor.getVendorId());
            entry.put("hotelName", vendor.getHotelName());
            entry.put("commissionRate", vendor.getCommissionRate());

            BigDecimal totalRevenue = commissionRecordRepository
                    .sumCommissionByVendor(vendor.getVendorId());
            long subscriptionCount = commissionRecordRepository
                    .findByVendorId(vendor.getVendorId()).size();

            entry.put("totalSubscriptionRevenue",
                    commissionRecordRepository.findByVendorId(vendor.getVendorId())
                            .stream().map(CommissionRecord::getSubscriptionAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add));
            entry.put("totalCommission", totalRevenue);
            entry.put("subscriptionCount", subscriptionCount);
            result.add(entry);
        }

        // Sort by commission (descending) = most valuable vendors first
        result.sort((a, b) -> {
            BigDecimal ca = (BigDecimal) a.get("totalCommission");
            BigDecimal cb = (BigDecimal) b.get("totalCommission");
            return cb.compareTo(ca);
        });
        return result;
    }

    // ── Commissions ──────────────────────────────────────────────────────────

    @GetMapping("/commissions")
    public Map<String, Object> commissionAnalytics() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("totalPending", commissionRecordRepository.sumCommissionByStatus("PENDING"));
        result.put("totalSettled", commissionRecordRepository.sumCommissionByStatus("SETTLED"));
        result.put("pendingCount", commissionRecordRepository.findByStatus("PENDING").size());
        result.put("settledCount", commissionRecordRepository.findByStatus("SETTLED").size());

        // Per-vendor commission breakdown
        List<VendorRecord> vendors = vendorRecordRepository.findAll();
        List<Map<String, Object>> vendorBreakdown = vendors.stream()
                .filter(v -> !commissionRecordRepository.findByVendorId(v.getVendorId()).isEmpty())
                .map(v -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("vendorId", v.getVendorId());
                    m.put("hotelName", v.getHotelName());
                    m.put("commissionRate", v.getCommissionRate());
                    m.put("totalCommission", commissionRecordRepository.sumCommissionByVendor(v.getVendorId()));
                    m.put("pendingCommission", commissionRecordRepository
                            .findByVendorIdAndStatus(v.getVendorId(), "PENDING")
                            .stream().map(CommissionRecord::getCommissionAmount)
                            .reduce(BigDecimal.ZERO, BigDecimal::add));
                    return m;
                })
                .collect(Collectors.toList());

        result.put("vendors", vendorBreakdown);
        return result;
    }

    // ── Orders / Redemptions ─────────────────────────────────────────────────

    @GetMapping("/orders")
    public Map<String, Object> orderAnalytics() {
        Map<String, Object> result = new LinkedHashMap<>();
        try {
            Object allRedemptions = restTemplate.getForObject(
                    redemptionServiceUrl + "/api/admin/redemptions/all", Object.class);
            List<Map<String, Object>> redemptions = extractList(allRedemptions);
            result.put("totalOrders", redemptions.size());
            result.put("totalOrderAmount", sumField(redemptions, "totalAmount"));

            // Group by store
            Map<Object, Long> byStore = redemptions.stream()
                    .collect(Collectors.groupingBy(
                            r -> r.getOrDefault("storeId", "unknown"),
                            Collectors.counting()));
            result.put("ordersByStore", byStore);

        } catch (Exception e) {
            result.put("error", "Could not fetch redemption data");
        }
        return result;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private int safeCount(String url) {
        try {
            Object data = restTemplate.getForObject(url, Object.class);
            if (data instanceof List<?> list) return list.size();
            if (data instanceof Map<?, ?> map) {
                Object inner = map.get("data");
                if (inner instanceof List<?> list) return list.size();
            }
        } catch (Exception ignored) {}
        return 0;
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
        return list.stream()
                .map(item -> item.get(field))
                .filter(v -> v instanceof Number)
                .map(v -> new BigDecimal(v.toString()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}