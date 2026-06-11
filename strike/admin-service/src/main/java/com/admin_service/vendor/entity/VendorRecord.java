package com.admin_service.vendor.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "admin_vendor_records")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class VendorRecord {

    @Id
    private Long vendorId;

    @Column(nullable = false)
    private String hotelName;

    private String mobileNumber;

    private String email;

    @Column(nullable = false)
    private String status;

    private String rejectionReason;

    // DB default allows ALTER TABLE to succeed on existing rows
    @Column(columnDefinition = "DECIMAL(5,2) DEFAULT 10.00")
    @Builder.Default
    private BigDecimal commissionRate = new BigDecimal("10.00");

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;

    @PrePersist
    @PreUpdate
    void ensureDefaults() {
        if (this.commissionRate == null) this.commissionRate = new BigDecimal("10.00");
    }
}