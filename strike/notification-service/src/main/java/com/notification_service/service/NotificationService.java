package com.notification_service.service;

import com.notification_service.dto.*;

public interface NotificationService {
    void sendOtp(OtpNotificationRequest request);
    void sendVendorStatus(VendorStatusNotificationRequest request);
    void sendSubscriptionConfirmation(SubscriptionNotificationRequest request);
    void sendRedemptionConfirmation(RedemptionNotificationRequest request);
}