package com.notification_service.controller;

import com.notification_service.dto.*;
import com.notification_service.entity.NotificationLog;
import com.notification_service.repository.NotificationLogRepository;
import com.notification_service.service.NotificationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/internal/notify")
@RequiredArgsConstructor
public class InternalNotificationController {

    private final NotificationService notificationService;
    private final NotificationLogRepository logRepository;

    @PostMapping("/otp")
    public ResponseEntity<String> sendOtp(@Valid @RequestBody OtpNotificationRequest request) {
        notificationService.sendOtp(request);
        return ResponseEntity.ok("OTP notification sent");
    }

    @PostMapping("/vendor-status")
    public ResponseEntity<String> sendVendorStatus(@Valid @RequestBody VendorStatusNotificationRequest request) {
        notificationService.sendVendorStatus(request);
        return ResponseEntity.ok("Vendor status notification sent");
    }

    @PostMapping("/subscription")
    public ResponseEntity<String> sendSubscription(@Valid @RequestBody SubscriptionNotificationRequest request) {
        notificationService.sendSubscriptionConfirmation(request);
        return ResponseEntity.ok("Subscription notification sent");
    }

    @PostMapping("/redemption")
    public ResponseEntity<String> sendRedemption(@Valid @RequestBody RedemptionNotificationRequest request) {
        notificationService.sendRedemptionConfirmation(request);
        return ResponseEntity.ok("Redemption notification sent");
    }

    // ── Admin: notification history ──────────────────────────────────────────

    @GetMapping("/logs")
    public List<NotificationLog> getAllLogs() {
        return logRepository.findAll();
    }

    @GetMapping("/logs/recipient/{recipientId}")
    public List<NotificationLog> getLogsByRecipient(
            @PathVariable Long recipientId,
            @RequestParam(defaultValue = "USER") String type) {
        return logRepository.findByRecipientIdAndRecipientTypeOrderByCreatedAtDesc(recipientId, type);
    }
}