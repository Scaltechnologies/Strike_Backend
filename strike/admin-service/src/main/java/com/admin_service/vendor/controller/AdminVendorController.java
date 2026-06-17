package com.admin_service.vendor.controller;

import com.admin_service.common.response.PageResponse;
import com.admin_service.vendor.entity.VendorRecord;
import com.admin_service.vendor.repository.VendorRecordRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/vendors")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'SUPER_ADMIN')")
public class AdminVendorController {

    private final VendorRecordRepository vendorRecordRepository;
    private final RestTemplate restTemplate;

    @Value("${services.auth-url}")
    private String authServiceUrl;

    @Value("${services.vendor-url}")
    private String vendorServiceUrl;

    @Value("${services.card-url}")
    private String cardServiceUrl;

    @Value("${services.redemption-url}")
    private String redemptionServiceUrl;

    @Value("${services.ledger-url}")
    private String ledgerServiceUrl;

    @Value("${services.notification-url}")
    private String notificationServiceUrl;

    // ── Vendor Listing ───────────────────────────────────────────────────────

    @GetMapping
    public PageResponse<VendorRecord> getAllVendors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PageResponse.from(vendorRecordRepository.findAll(PageRequest.of(page, size)));
    }

    @GetMapping("/pending")
    public PageResponse<VendorRecord> getPendingVendors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PageResponse.from(vendorRecordRepository.findByStatus("PENDING", PageRequest.of(page, size)));
    }

    @GetMapping("/active")
    public PageResponse<VendorRecord> getActiveVendors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PageResponse.from(vendorRecordRepository.findByStatus("ACTIVE", PageRequest.of(page, size)));
    }

    @GetMapping("/suspended")
    public PageResponse<VendorRecord> getSuspendedVendors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PageResponse.from(vendorRecordRepository.findByStatus("SUSPENDED", PageRequest.of(page, size)));
    }

    @GetMapping("/rejected")
    public PageResponse<VendorRecord> getRejectedVendors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PageResponse.from(vendorRecordRepository.findByStatus("REJECTED", PageRequest.of(page, size)));
    }

    @GetMapping("/{vendorId}")
    public ResponseEntity<VendorRecord> getVendor(@PathVariable Long vendorId) {
        return vendorRecordRepository.findById(vendorId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    // ── Vendor Status Actions ────────────────────────────────────────────────

    @PatchMapping("/{vendorId}/approve")
    public ResponseEntity<String> approveVendor(@PathVariable Long vendorId) {
        VendorRecord record = vendorRecordRepository.findById(vendorId).orElse(null);
        if (record == null) return ResponseEntity.notFound().build();

        record.setStatus("ACTIVE");
        record.setRejectionReason(null);
        vendorRecordRepository.save(record);

        try {
            restTemplate.patchForObject(
                    authServiceUrl + "/internal/vendors/" + vendorId + "/approve", null, Void.class);
        } catch (Exception e) {
            return ResponseEntity.ok("Vendor approved in admin. Auth-service sync failed: " + e.getMessage());
        }
        sendVendorNotification(record, "APPROVED", null);
        return ResponseEntity.ok("Vendor approved");
    }

    @PatchMapping("/{vendorId}/reject")
    public ResponseEntity<String> rejectVendor(
            @PathVariable Long vendorId,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.getOrDefault("reason", "") : "";
        VendorRecord record = vendorRecordRepository.findById(vendorId).orElse(null);
        if (record == null) return ResponseEntity.notFound().build();

        record.setStatus("REJECTED");
        record.setRejectionReason(reason);
        vendorRecordRepository.save(record);

        try {
            restTemplate.patchForObject(
                    authServiceUrl + "/internal/vendors/" + vendorId + "/reject", null, Void.class);
        } catch (Exception ignored) {}
        sendVendorNotification(record, "REJECTED", reason);
        return ResponseEntity.ok("Vendor rejected");
    }

    @PatchMapping("/{vendorId}/suspend")
    public ResponseEntity<String> suspendVendor(
            @PathVariable Long vendorId,
            @RequestBody(required = false) Map<String, String> body) {
        String reason = body != null ? body.getOrDefault("reason", "") : "";
        VendorRecord record = vendorRecordRepository.findById(vendorId).orElse(null);
        if (record == null) return ResponseEntity.notFound().build();

        record.setStatus("SUSPENDED");
        record.setRejectionReason(reason);
        vendorRecordRepository.save(record);

        try {
            restTemplate.patchForObject(
                    authServiceUrl + "/internal/vendors/" + vendorId + "/suspend", null, Void.class);
        } catch (Exception ignored) {}
        sendVendorNotification(record, "SUSPENDED", reason);
        return ResponseEntity.ok("Vendor suspended");
    }

    @PatchMapping("/{vendorId}/reactivate")
    public ResponseEntity<String> reactivateVendor(@PathVariable Long vendorId) {
        VendorRecord record = vendorRecordRepository.findById(vendorId).orElse(null);
        if (record == null) return ResponseEntity.notFound().build();

        record.setStatus("ACTIVE");
        record.setRejectionReason(null);
        vendorRecordRepository.save(record);

        try {
            restTemplate.patchForObject(
                    authServiceUrl + "/internal/vendors/" + vendorId + "/reactivate", null, Void.class);
        } catch (Exception ignored) {}
        sendVendorNotification(record, "REACTIVATED", null);
        return ResponseEntity.ok("Vendor reactivated");
    }

    // ── Commission Rate ──────────────────────────────────────────────────────

    @PatchMapping("/{vendorId}/commission-rate")
    public ResponseEntity<String> updateCommissionRate(
            @PathVariable Long vendorId,
            @RequestBody Map<String, Object> body) {
        VendorRecord record = vendorRecordRepository.findById(vendorId).orElse(null);
        if (record == null) return ResponseEntity.notFound().build();
        Object rate = body.get("commissionRate");
        if (rate == null) return ResponseEntity.badRequest().body("commissionRate is required");
        record.setCommissionRate(new java.math.BigDecimal(rate.toString()));
        vendorRecordRepository.save(record);
        return ResponseEntity.ok("Commission rate updated to " + rate + "% for vendor " + vendorId);
    }

    // ── Vendor Data Queries ──────────────────────────────────────────────────

    @GetMapping("/{vendorId}/store")
    public Object getVendorStore(@PathVariable Long vendorId) {
        return restTemplate.getForObject(
                vendorServiceUrl + "/internal/vendors/" + vendorId + "/store", Object.class);
    }

    @GetMapping("/{vendorId}/cards")
    public Object getVendorCards(@PathVariable Long vendorId) {
        return restTemplate.getForObject(
                cardServiceUrl + "/api/admin/cards/vendor/" + vendorId, Object.class);
    }

    @GetMapping("/{vendorId}/subscriptions")
    public Object getVendorSubscriptions(@PathVariable Long vendorId) {
        Long storeId = resolveStoreId(vendorId);
        if (storeId == null) return Map.of("error", "No store found for vendor");
        return restTemplate.getForObject(
                cardServiceUrl + "/api/admin/cards/subscriptions/store/" + storeId, Object.class);
    }

    @GetMapping("/{vendorId}/redemptions")
    public Object getVendorRedemptions(@PathVariable Long vendorId) {
        Long storeId = resolveStoreId(vendorId);
        if (storeId == null) return Map.of("error", "No store found for vendor");
        return restTemplate.getForObject(
                redemptionServiceUrl + "/api/admin/redemptions/store/" + storeId, Object.class);
    }

    @GetMapping("/{vendorId}/transactions")
    public Object getVendorTransactions(@PathVariable Long vendorId) {
        Long storeId = resolveStoreId(vendorId);
        if (storeId == null) return Map.of("error", "No store found for vendor");
        return restTemplate.getForObject(
                ledgerServiceUrl + "/api/ledger/store/" + storeId, Object.class);
    }

    private void sendVendorNotification(VendorRecord record, String status, String reason) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("vendorId", record.getVendorId());
            payload.put("mobile", record.getMobileNumber());
            payload.put("email", record.getEmail());
            payload.put("hotelName", record.getHotelName());
            payload.put("status", status);
            payload.put("reason", reason);
            restTemplate.postForObject(
                    notificationServiceUrl + "/internal/notify/vendor-status", payload, String.class);
        } catch (Exception e) {
            log.warn("Notification service unavailable for vendor status {}: {}", status, e.getMessage());
        }
    }

    @SuppressWarnings("unchecked")
    private Long resolveStoreId(Long vendorId) {
        try {
            Object storeData = restTemplate.getForObject(
                    vendorServiceUrl + "/internal/vendors/" + vendorId + "/store", Object.class);
            if (storeData instanceof Map<?, ?> map) {
                Object id = map.get("id");
                if (id instanceof Number n) return n.longValue();
            }
        } catch (Exception ignored) {}
        return null;
    }
}