package com.user_service.savedstore.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
    name = "user_saved_stores",
    uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "store_id"})
)
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserSavedStore {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "store_id", nullable = false)
    private Long storeId;

    @CreationTimestamp
    private LocalDateTime savedAt;
}