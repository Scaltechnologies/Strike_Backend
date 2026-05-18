package com.vendor_service.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "vendor_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VendorProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long vendorId;

    @Column(nullable = false)
    private String shopName;

    private String ownerName;

    @Column(unique = true)
    private String mobile;

    private String address;

    private String category;

    @Column(length = 1000)
    private String description;

    private String logoUrl;
}