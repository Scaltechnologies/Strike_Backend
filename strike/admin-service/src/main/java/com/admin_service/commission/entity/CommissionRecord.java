package com.admin_service.commission.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "commission_records")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CommissionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long vendorId;

    @Column(nullable = false)
    private Long storeId;

    @Column(nullable = false)
    private Long subscriptionId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal subscriptionAmount;

    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal commissionRate;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal commissionAmount;

    @Column(nullable = false)
    @Builder.Default
    private String status = "PENDING";

    private LocalDateTime settledAt;

    @CreationTimestamp
    private LocalDateTime createdAt;
}