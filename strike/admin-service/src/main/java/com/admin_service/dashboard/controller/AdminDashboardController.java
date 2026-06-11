package com.admin_service.dashboard.controller;

import com.admin_service.commission.repository.CommissionRecordRepository;
import com.admin_service.vendor.entity.VendorRecord;
import com.admin_service.vendor.repository.VendorRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.*;

@RestController
@RequestMapping("/api/admin/dashboard")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminDashboardController {

    private final VendorRecordRepository vendorRecordRepository;
    private final CommissionRecordRepository commissionRecordRepository;
    private final RestTemplate restTemplate;

    @Value("${services.auth-url}")
    private String authServiceUrl;

    @Value("${services.redemption-url}")
    private String redemptionServiceUrl;

    @GetMapping
    public Map<String, Object> getDashboard() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        Map<String, Object> dashboard = new LinkedHashMap<>();

        // Admin info from SecurityContext (set by AdminJwtFilter)
        dashboard.put("admin", Map.of(
                "email", auth.getPrincipal(),
                "role", auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "")
        ));

        // ── Platform stats ────────────────────────────────────────────────────
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalUsers", safeCount(authServiceUrl + "/internal/users"));

        Map<String, Object> vendorStats = new LinkedHashMap<>();
        vendorStats.put("total",     vendorRecordRepository.count());
        vendorStats.put("pending",   vendorRecordRepository.findByStatus("PENDING").size());
        vendorStats.put("active",    vendorRecordRepository.findByStatus("ACTIVE").size());
        vendorStats.put("suspended", vendorRecordRepository.findByStatus("SUSPENDED").size());
        vendorStats.put("rejected",  vendorRecordRepository.findByStatus("REJECTED").size());
        stats.put("vendors", vendorStats);

        BigDecimal totalRevenue     = commissionRecordRepository.totalSubscriptionRevenue();
        BigDecimal totalCommission  = commissionRecordRepository.sumCommissionByStatus("PENDING")
                                        .add(commissionRecordRepository.sumCommissionByStatus("SETTLED"));
        BigDecimal pendingCommission = commissionRecordRepository.sumCommissionByStatus("PENDING");

        stats.put("totalSubscriptionRevenue", totalRevenue);
        stats.put("totalCommissionEarned",    totalCommission);
        stats.put("pendingCommission",         pendingCommission);
        stats.put("totalSubscriptions",        commissionRecordRepository.count());
        stats.put("totalRedemptions",          safeCount(redemptionServiceUrl + "/api/admin/redemptions/all"));
        dashboard.put("stats", stats);

        // ── Pending vendor approvals (up to 10) ───────────────────────────────
        List<Map<String, Object>> pendingApprovals = vendorRecordRepository.findByStatus("PENDING")
                .stream()
                .limit(10)
                .map(v -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("vendorId",    v.getVendorId());
                    m.put("hotelName",   v.getHotelName());
                    m.put("mobile",      v.getMobileNumber());
                    m.put("email",       v.getEmail());
                    m.put("registeredAt", v.getCreatedAt() != null ? v.getCreatedAt().toString() : "");
                    return m;
                }).toList();
        dashboard.put("pendingApprovals", pendingApprovals);

        // ── Recent vendor registrations (last 5) ──────────────────────────────
        List<VendorRecord> all = vendorRecordRepository.findAll();
        all.sort(Comparator.comparing(VendorRecord::getCreatedAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
        List<Map<String, Object>> recentVendors = all.stream()
                .limit(5)
                .map(v -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("vendorId",    v.getVendorId());
                    m.put("hotelName",   v.getHotelName());
                    m.put("status",      v.getStatus());
                    m.put("registeredAt", v.getCreatedAt() != null ? v.getCreatedAt().toString() : "");
                    return m;
                }).toList();
        dashboard.put("recentVendorRegistrations", recentVendors);

        return dashboard;
    }

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
}