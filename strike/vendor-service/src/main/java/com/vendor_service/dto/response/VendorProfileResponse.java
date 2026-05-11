
package com.vendor_service.dto.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class VendorProfileResponse {

    private UUID vendorId;

    private String hotelName;

    private String address;

    private String email;

    private Double latitude;

    private Double longitude;

    private String description;

    private String cuisineType;

    private String profileImage;

    private LocalDateTime createdAt;

}

