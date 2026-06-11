package com.vendor_service.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class VendorProfileResponse {

    private Long id;
    private Long vendorId;
    private String shopName;
    private String ownerName;
    private String mobile;
    private String email;
    private String address;
    private String category;
    private String description;
    private String logoUrl;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}