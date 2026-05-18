package com.vendor_service.dto.response;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class VendorProfileResponse {

    private Long id;

    private Long vendorId;

    private String shopName;

    private String ownerName;

    private String mobile;

    private String address;

    private String category;

    private String description;

    private String logoUrl;
}