package com.notification_service.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notification_logs")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class NotificationLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private Long recipientId;

    /** USER, VENDOR, ADMIN */
    @Column(nullable = false)
    private String recipientType;

    private String recipientMobile;

    private String recipientEmail;

    /** OTP, VENDOR_APPROVED, VENDOR_REJECTED, VENDOR_SUSPENDED, VENDOR_REACTIVATED,
     *  SUBSCRIPTION_PURCHASED, REDEMPTION_PROCESSED, LOW_BALANCE */
    @Column(nullable = false)
    private String type;

    /** SMS, EMAIL */
    @Column(nullable = false)
    private String channel;

    @Column(columnDefinition = "TEXT")
    private String message;

    /** SENT, FAILED, MOCK */
    @Column(nullable = false)
    private String status;

    private String errorDetail;

    @CreationTimestamp
    private LocalDateTime createdAt;
}