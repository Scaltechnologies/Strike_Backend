package com.admin_service.dashboard.controller;

import com.admin_service.commission.repository.CommissionRecordRepository;
import com.admin_service.vendor.entity.VendorRecord;
import com.admin_service.vendor.repository.VendorRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
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

        dashboard.put("admin", Map.of(
                "email", auth.getPrincipal(),
                "role", auth.getAuthorities().iterator().next().getAuthority().replace("ROLE_", "")
        ));

        // ── Platform stats ────────────────────────────────────────────────────
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalUsers", safeCountEndpoint(authServiceUrl + "/internal/users/count"));

        Map<String, Object> vendorStats = new LinkedHashMap<>();
        vendorStats.put("total",     vendorRecordRepository.count());
        vendorStats.put("pending",   vendorRecordRepository.countByStatus("PENDING"));
        vendorStats.put("active",    vendorRecordRepository.countByStatus("ACTIVE"));
        vendorStats.put("suspended", vendorRecordRepository.countByStatus("SUSPENDED"));
        vendorStats.put("rejected",  vendorRecordRepository.countByStatus("REJECTED"));
        stats.put("vendors", vendorStats);

        BigDecimal totalRevenue     = commissionRecordRepository.totalSubscriptionRevenue();
        BigDecimal totalCommission  = commissionRecordRepository.sumCommissionByStatus("PENDING")
                                        .add(commissionRecordRepository.sumCommissionByStatus("SETTLED"));
        BigDecimal pendingCommission = commissionRecordRepository.sumCommissionByStatus("PENDING");

        stats.put("totalSubscriptionRevenue", totalRevenue);
        stats.put("totalCommissionEarned",    totalCommission);
        stats.put("pendingCommission",         pendingCommission);
        stats.put("totalSubscriptions",        commissionRecordRepository.count());
        stats.put("totalRedemptions",          safeTotalElements(redemptionServiceUrl + "/api/admin/redemptions/all?size=1"));
        dashboard.put("stats", stats);

        // ── Pending vendor approvals (up to 10) ───────────────────────────────
        List<Map<String, Object>> pendingApprovals = vendorRecordRepository
                .findByStatus("PENDING", PageRequest.of(0, 10))
                .getContent()
                .stream()
                .map(v -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("vendorId",     v.getVendorId());
                    m.put("hotelName",    v.getHotelName());
                    m.put("mobile",       v.getMobileNumber());
                    m.put("email",        v.getEmail());
                    m.put("registeredAt", v.getCreatedAt() != null ? v.getCreatedAt().toString() : "");
                    return m;
                }).toList();
        dashboard.put("pendingApprovals", pendingApprovals);

        // ── Recent vendor registrations (last 5) ──────────────────────────────
        List<Map<String, Object>> recentVendors = vendorRecordRepository
                .findAll(PageRequest.of(0, 5, Sort.by(Sort.Direction.DESC, "createdAt")))
                .getContent()
                .stream()
                .map(v -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("vendorId",     v.getVendorId());
                    m.put("hotelName",    v.getHotelName());
                    m.put("status",       v.getStatus());
                    m.put("registeredAt", v.getCreatedAt() != null ? v.getCreatedAt().toString() : "");
                    return m;
                }).toList();
        dashboard.put("recentVendorRegistrations", recentVendors);

        return dashboard;
    }

    /** Reads a plain numeric count from a dedicated count endpoint. */
    private long safeCountEndpoint(String url) {
        try {
            Object result = restTemplate.getForObject(url, Object.class);
            if (result instanceof Number n) return n.longValue();
        } catch (Exception ignored) {}
        return 0L;
    }

    /**
     * Reads totalElements from a paginated endpoint that returns
     * { "data": { "totalElements": N, ... }, ... } (ApiResponse wrapping PageResponse).
     */
    @SuppressWarnings("unchecked")
    private long safeTotalElements(String url) {
        try {
            Object data = restTemplate.getForObject(url, Object.class);
            if (data instanceof Map<?, ?> root) {
                Object inner = root.get("data");
                if (inner instanceof Map<?, ?> page) {
                    Object total = page.get("totalElements");
                    if (total instanceof Number n) return n.longValue();
                }
            }
        } catch (Exception ignored) {}
        return 0L;
    }
}