package com.notification_service.service.impl;

import com.notification_service.dto.*;
import com.notification_service.entity.NotificationLog;
import com.notification_service.repository.NotificationLogRepository;
import com.notification_service.service.EmailService;
import com.notification_service.service.NotificationService;
import com.notification_service.service.SmsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.format.DateTimeFormatter;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final SmsService smsService;
    private final EmailService emailService;
    private final NotificationLogRepository logRepository;
    private final RestTemplate restTemplate;

    @Value("${services.auth-url}")
    private String authServiceUrl;

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    // ── OTP ─────────────────────────────────────────────────────────────────

    @Override
    public void sendOtp(OtpNotificationRequest req) {
        String message = "Your Strike OTP is: " + req.getOtp() + ". Valid for 5 minutes. Do not share this code.";
        String status = smsService.send(req.getMobile(), message);
        saveLog(null, req.getRecipientType(), req.getMobile(), null,
                "OTP", "SMS", message, status, null);
    }

    // ── Vendor Status ────────────────────────────────────────────────────────

    @Override
    public void sendVendorStatus(VendorStatusNotificationRequest req) {
        String smsMessage;
        String emailSubject;
        String emailBody;
        String type = "VENDOR_" + req.getStatus();

        switch (req.getStatus()) {
            case "APPROVED" -> {
                smsMessage = "Congratulations! Your Strike vendor account for " + req.getHotelName()
                        + " has been approved. You can now log in and start accepting orders.";
                emailSubject = "Strike — Vendor Account Approved";
                emailBody = "Dear " + req.getHotelName() + ",\n\n"
                        + "Your vendor account has been approved! You can now log in to the Strike app and start setting up your menu and accepting orders.\n\n"
                        + "Welcome to the Strike platform!\n\nTeam Strike";
            }
            case "REJECTED" -> {
                smsMessage = "Your Strike vendor registration for " + req.getHotelName() + " was rejected."
                        + (req.getReason() != null ? " Reason: " + req.getReason() : "") + " Contact support for help.";
                emailSubject = "Strike — Vendor Registration Update";
                emailBody = "Dear " + req.getHotelName() + ",\n\n"
                        + "Unfortunately, your vendor registration has been rejected.\n"
                        + (req.getReason() != null ? "Reason: " + req.getReason() + "\n" : "")
                        + "\nPlease contact our support team for assistance.\n\nTeam Strike";
            }
            case "SUSPENDED" -> {
                smsMessage = "Your Strike vendor account for " + req.getHotelName() + " has been suspended."
                        + (req.getReason() != null ? " Reason: " + req.getReason() : "") + " Contact support.";
                emailSubject = "Strike — Vendor Account Suspended";
                emailBody = "Dear " + req.getHotelName() + ",\n\n"
                        + "Your vendor account has been suspended.\n"
                        + (req.getReason() != null ? "Reason: " + req.getReason() + "\n" : "")
                        + "\nPlease contact our support team to resolve this.\n\nTeam Strike";
            }
            case "REACTIVATED" -> {
                smsMessage = "Your Strike vendor account for " + req.getHotelName() + " has been reactivated. Welcome back!";
                emailSubject = "Strike — Vendor Account Reactivated";
                emailBody = "Dear " + req.getHotelName() + ",\n\n"
                        + "Your vendor account has been reactivated. You can now log in and start accepting orders again.\n\n"
                        + "Welcome back!\n\nTeam Strike";
            }
            default -> {
                log.warn("Unknown vendor status for notification: {}", req.getStatus());
                return;
            }
        }

        String smsStatus = smsService.send(req.getMobile(), smsMessage);
        saveLog(req.getVendorId(), "VENDOR", req.getMobile(), null, type, "SMS", smsMessage, smsStatus, null);

        if (req.getEmail() != null && !req.getEmail().isBlank()) {
            String emailStatus = emailService.send(req.getEmail(), emailSubject, emailBody);
            saveLog(req.getVendorId(), "VENDOR", null, req.getEmail(), type, "EMAIL", emailBody, emailStatus, null);
        }
    }

    // ── Subscription Confirmation ────────────────────────────────────────────

    @Override
    public void sendSubscriptionConfirmation(SubscriptionNotificationRequest req) {
        String mobile = resolveMobile(req.getUserId());
        if (mobile == null) {
            log.warn("Could not resolve mobile for userId={} — subscription notification skipped", req.getUserId());
            return;
        }

        String expiryStr = req.getExpiresAt() != null ? req.getExpiresAt().format(DATE_FMT) : "N/A";
        String storePart = (req.getStoreName() != null && !req.getStoreName().isBlank())
                ? " at " + req.getStoreName() : "";
        String message = "You have subscribed to " + req.getCardName() + storePart
                + ". Wallet balance: Rs." + req.getWalletBalance()
                + ". Valid till: " + expiryStr + ". Enjoy your meals!";

        String status = smsService.send(mobile, message);
        saveLog(req.getUserId(), "USER", mobile, null,
                "SUBSCRIPTION_PURCHASED", "SMS", message, status, null);
    }

    // ── Redemption Confirmation ──────────────────────────────────────────────

    @Override
    public void sendRedemptionConfirmation(RedemptionNotificationRequest req) {
        String mobile = resolveMobile(req.getUserId());
        if (mobile == null) {
            log.warn("Could not resolve mobile for userId={} — redemption notification skipped", req.getUserId());
            return;
        }

        String storePart = (req.getStoreName() != null && !req.getStoreName().isBlank())
                ? " at " + req.getStoreName() : "";
        String message = "Rs." + req.getTotalAmount() + " redeemed" + storePart
                + ". Remaining wallet balance: Rs." + req.getRemainingBalance() + ". Bon appetit!";

        String status = smsService.send(mobile, message);
        saveLog(req.getUserId(), "USER", mobile, null,
                "REDEMPTION_PROCESSED", "SMS", message, status, null);

        // Low balance alert (below 10% of typical balance or below 100)
        if (req.getRemainingBalance() != null
                && req.getRemainingBalance().compareTo(new java.math.BigDecimal("100")) <= 0) {
            String lowBalanceMsg = "Low balance alert! Your Strike wallet has Rs." + req.getRemainingBalance()
                    + " remaining. Consider recharging to continue enjoying your meals.";
            String lbStatus = smsService.send(mobile, lowBalanceMsg);
            saveLog(req.getUserId(), "USER", mobile, null,
                    "LOW_BALANCE", "SMS", lowBalanceMsg, lbStatus, null);
        }
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    @SuppressWarnings("unchecked")
    private String resolveMobile(Long userId) {
        try {
            Map<?, ?> user = restTemplate.getForObject(
                    authServiceUrl + "/internal/users/" + userId, Map.class);
            if (user != null && user.get("mobileNumber") != null) {
                return user.get("mobileNumber").toString();
            }
        } catch (Exception e) {
            log.warn("Failed to resolve mobile for userId={}: {}", userId, e.getMessage());
        }
        return null;
    }

    private void saveLog(Long recipientId, String recipientType, String mobile, String email,
                         String type, String channel, String message, String status, String error) {
        try {
            logRepository.save(NotificationLog.builder()
                    .recipientId(recipientId)
                    .recipientType(recipientType)
                    .recipientMobile(mobile)
                    .recipientEmail(email)
                    .type(type)
                    .channel(channel)
                    .message(message)
                    .status(status)
                    .errorDetail(error)
                    .build());
        } catch (Exception e) {
            log.error("Failed to save notification log: {}", e.getMessage());
        }
    }
}