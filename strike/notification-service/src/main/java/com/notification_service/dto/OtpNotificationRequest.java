package com.notification_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class OtpNotificationRequest {

    @NotBlank(message = "Mobile number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Mobile number must be a valid 10-digit Indian number")
    private String mobile;

    @NotBlank(message = "OTP is required")
    private String otp;

    @NotBlank(message = "Recipient type is required")
    @Pattern(regexp = "USER|VENDOR", message = "Recipient type must be USER or VENDOR")
    private String recipientType;
}