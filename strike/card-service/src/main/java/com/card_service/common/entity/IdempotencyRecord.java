package com.card_service.common.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "subscription_idempotency",
        indexes = @Index(name = "idx_sub_idempotency_key", columnList = "idempotency_key"))
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class IdempotencyRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "idempotency_key", nullable = false, unique = true, length = 100)
    private String idempotencyKey;

    @Column(columnDefinition = "TEXT") // null = reserved/in-flight, non-null = complete
    private String responseBody;

    private int httpStatus;

    @Column(nullable = false)
    private LocalDateTime createdAt;
}