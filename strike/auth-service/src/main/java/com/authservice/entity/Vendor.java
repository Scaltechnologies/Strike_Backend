package com.authservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "vendors", schema = "auth_service")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vendor {

    @Id
    private UUID id;

    @Column(name = "hotel_name")
    private String hotelName;

    private String address;

    @Column(name = "mobile_number", unique = true)
    private String mobileNumber;

    private String email;

    private Double latitude;

    private Double longitude;

    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}