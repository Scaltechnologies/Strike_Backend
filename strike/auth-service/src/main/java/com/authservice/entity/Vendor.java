package com.authservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "vendors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Vendor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "hotel_name")
    private String hotelName;

    private String address;

    @Column(name = "mobile_number")
    private String mobileNumber;

    private String email;

    private Double latitude;

    private Double longitude;

    private String status;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}