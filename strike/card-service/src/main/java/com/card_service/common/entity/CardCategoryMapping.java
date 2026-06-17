package com.card_service.common.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "card_category_mappings",
       uniqueConstraints = @UniqueConstraint(columnNames = {"cardDefinitionId", "categoryId"}))
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CardCategoryMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long cardDefinitionId;

    @Column(nullable = false)
    private Long categoryId;

    @CreationTimestamp
    private LocalDateTime createdAt;
}