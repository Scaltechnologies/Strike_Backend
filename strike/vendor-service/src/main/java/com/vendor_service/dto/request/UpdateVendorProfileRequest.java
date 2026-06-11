package com.vendor_service.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UpdateVendorProfileRequest {

    @Size(min = 2, max = 100, message = "Shop name must be 2-100 characters")
    private String shopName;

    @Size(max = 100)
    private String ownerName;

    @Size(max = 15)
    private String mobile;

    @Email(message = "Invalid email format")
    private String email;

    @Size(max = 300)
    private String address;

    @Size(max = 100)
    private String category;

    @Size(max = 1000)
    private String description;

    private String logoUrl;
}