package com.admin_service.analytics.controller;

import com.admin_service.commission.entity.CommissionRecord;
import com.admin_service.commission.repository.CommissionRecordRepository;
import com.admin_service.vendor.entity.VendorRecord;
import com.admin_service.vendor.repository.VendorRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
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
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
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

        result.put("totalUsers", safeLong(authServiceUrl + "/internal/users/count"));

        Map<String, Object> vendorBreakdown = new LinkedHashMap<>();
        vendorBreakdown.put("total",     vendorRecordRepository.count());
        vendorBreakdown.put("active",    vendorRecordRepository.countByStatus("ACTIVE"));
        vendorBreakdown.put("pending",   vendorRecordRepository.countByStatus("PENDING"));
        vendorBreakdown.put("suspended", vendorRecordRepository.countByStatus("SUSPENDED"));
        result.put("vendors", vendorBreakdown);

        result.put("totalSubscriptions", commissionRecordRepository.count());
        result.put("totalSubscriptionRevenue", commissionRecordRepository.totalSubscriptionRevenue());
        result.put("totalCommissionEarned",
                commissionRecordRepository.sumCommissionByStatus("PENDING")
                        .add(commissionRecordRepository.sumCommissionByStatus("SETTLED")));
        result.put("pendingCommission", commissionRecordRepository.sumCommissionByStatus("PENDING"));
        result.put("totalRedemptions", safeStatsField(redemptionServiceUrl + "/api/admin/redemptions/stats", "totalRedemptions"));

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
        List<VendorRecord> vendors = vendorRecordRepository.findByStatus("ACTIVE", Pageable.unpaged()).getContent();
        List<Map<String, Object>> result = new ArrayList<>();

        for (VendorRecord vendor : vendors) {
            Map<String, Object> entry = new LinkedHashMap<>();
            entry.put("vendorId", vendor.getVendorId());
            entry.put("hotelName", vendor.getHotelName());
            entry.put("commissionRate", vendor.getCommissionRate());
            entry.put("totalSubscriptionRevenue",
                    commissionRecordRepository.sumSubscriptionAmountByVendor(vendor.getVendorId()));
            entry.put("totalCommission",
                    commissionRecordRepository.sumCommissionByVendor(vendor.getVendorId()));
            entry.put("subscriptionCount",
                    commissionRecordRepository.countByVendorId(vendor.getVendorId()));
            result.add(entry);
        }

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
        result.put("pendingCount", commissionRecordRepository.countByStatus("PENDING"));
        result.put("settledCount", commissionRecordRepository.countByStatus("SETTLED"));

        List<VendorRecord> vendors = vendorRecordRepository.findAll(Pageable.unpaged()).getContent();
        List<Map<String, Object>> vendorBreakdown = vendors.stream()
                .filter(v -> commissionRecordRepository.countByVendorId(v.getVendorId()) > 0)
                .map(v -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("vendorId", v.getVendorId());
                    m.put("hotelName", v.getHotelName());
                    m.put("commissionRate", v.getCommissionRate());
                    m.put("totalCommission", commissionRecordRepository.sumCommissionByVendor(v.getVendorId()));
                    m.put("pendingCommission",
                            commissionRecordRepository.findByVendorIdAndStatus(v.getVendorId(), "PENDING")
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
            Object statsData = restTemplate.getForObject(
                    redemptionServiceUrl + "/api/admin/redemptions/stats", Object.class);
            if (statsData instanceof Map<?, ?> stats) {
                result.put("totalOrders", stats.get("totalRedemptions"));
                result.put("totalOrderAmount", stats.get("totalAmount"));
            }
        } catch (Exception e) {
            result.put("error", "Could not fetch redemption data");
        }
        return result;
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private long safeLong(String url) {
        try {
            Object data = restTemplate.getForObject(url, Object.class);
            if (data instanceof Number n) return n.longValue();
        } catch (Exception ignored) {}
        return 0L;
    }

    @SuppressWarnings("unchecked")
    private Object safeStatsField(String url, String field) {
        try {
            Object data = restTemplate.getForObject(url, Object.class);
            if (data instanceof Map<?, ?> map) return map.get(field);
        } catch (Exception ignored) {}
        return 0;
    }
}
