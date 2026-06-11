package com.notification_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class VendorStatusNotificationRequest {

    @NotNull(message = "Vendor ID is required")
    private Long vendorId;

    @NotBlank(message = "Mobile number is required")
    private String mobile;

    private String email;

    private String hotelName;

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "APPROVED|REJECTED|SUSPENDED|REACTIVATED", message = "Status must be APPROVED, REJECTED, SUSPENDED, or REACTIVATED")
    private String status;

    private String reason;
}