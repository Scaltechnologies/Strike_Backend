package com.vendor_service.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateVendorProfileRequest {

    private String shopName;

    private String ownerName;

    private String mobile;

    private String address;

    private String category;

    private String description;

    private String logoUrl;
}