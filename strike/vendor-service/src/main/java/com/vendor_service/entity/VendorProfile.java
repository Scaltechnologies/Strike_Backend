package com.vendor_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "vendor_profiles")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorProfile {

    @Id
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

    private LocalDateTime updatedAt;
}

