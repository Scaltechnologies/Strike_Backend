package com.redemption_service.common.entity;

import com.redemption_service.common.enums.RedemptionStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "redemption_records")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class RedemptionRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Prevents double-approval under concurrent vendor requests
    @Version
    @Column(columnDefinition = "bigint default 0")
    private Long version;

    @Column(nullable = false)
    private Long subscriptionId;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long storeId;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal totalAmount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RedemptionStatus status;

    // "USER" for user-initiated requests; "VENDOR" for legacy POS-direct redemptions
    private String initiatedBy;

    // Reused as rejection reason when status = REJECTED
    private String failureReason;

    private LocalDateTime approvedAt;

    // Balance remaining in the subscription wallet after vendor approval.
    // Stored so GET /api/redemptions/{id} can return it without a card-service round-trip.
    private BigDecimal approvedBalance;

    private LocalDateTime rejectedAt;

    @OneToMany(mappedBy = "redemptionRecord", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Builder.Default
    private List<RedemptionItem> items = new ArrayList<>();

    @CreationTimestamp
    private LocalDateTime createdAt;
}