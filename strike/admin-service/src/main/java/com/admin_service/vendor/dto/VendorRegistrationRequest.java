package com.admin_service.vendor.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class VendorRegistrationRequest {

    @NotNull(message = "Vendor ID is required")
    private Long vendorId;

    @NotBlank(message = "Hotel name is required")
    private String hotelName;

    @NotBlank(message = "Mobile number is required")
    private String mobileNumber;

    @Email(message = "Invalid email format")
    private String email;

    private String status;
}