package com.vendor_service.dto.request;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class InitVendorProfileRequest {

    @NotNull(message = "Vendor ID is required")
    private Long vendorId;

    @NotBlank(message = "Shop name is required")
    private String shopName;

    @NotBlank(message = "Mobile number is required")
    private String mobile;

    @NotBlank(message = "Address is required")
    private String address;

    @Email(message = "Invalid email format")
    private String email;

    @DecimalMin(value = "-90.0",  message = "Latitude must be >= -90")
    @DecimalMax(value = "90.0",   message = "Latitude must be <= 90")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "Longitude must be >= -180")
    @DecimalMax(value = "180.0",  message = "Longitude must be <= 180")
    private Double longitude;
}