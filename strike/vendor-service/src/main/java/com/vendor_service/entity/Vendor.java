package com.vendor_service.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "vendors")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Vendor {

    @Id
    private UUID id;

    private String hotelName;

    private String address;

    private String mobileNumber;

    private String email;

    private Double latitude;

    private Double longitude;

    private String status;

    private LocalDateTime createdAt;
}
