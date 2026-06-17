package com.card_service.common.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "card_menu_item_mappings",
       uniqueConstraints = @UniqueConstraint(columnNames = {"cardDefinitionId", "menuItemId"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CardMenuItemMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long cardDefinitionId;

    @Column(nullable = false)
    private Long menuItemId;

    @CreationTimestamp
    private LocalDateTime createdAt;
}