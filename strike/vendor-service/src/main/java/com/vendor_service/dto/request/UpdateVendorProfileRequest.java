package com.vendor_service.dto.request;

import lombok.Data;

@Data
public class UpdateVendorProfileRequest {

    private String hotelName;

    private String address;

    private String email;

    private Double latitude;

    private Double longitude;

    private String description;

    private String cuisineType;

}

