package com.user_service.profile.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import org.springframework.data.domain.Persistable;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_profiles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class UserProfile implements Persistable<Long> {

    @Id
    private Long userId;

    /**
     * Tracks whether this instance has ever been persisted.
     * Required because userId is a manually assigned @Id (no @GeneratedValue):
     * Spring Data's isNew() would see a non-null ID and call merge() instead of
     * persist(), which in Hibernate 6 does not reliably issue an INSERT for a
     * brand-new entity. By implementing Persistable, we force persist() when the
     * entity is built fresh via the builder.
     */
    @Transient
    @Builder.Default
    private boolean isNew = true;

    @Override
    public Long getId() {
        return userId;
    }

    @Override
    public boolean isNew() {
        return isNew;
    }

    @PostPersist
    @PostLoad
    private void markNotNew() {
        this.isNew = false;
    }

    private String name;

    private String email;

    private String profilePicUrl;

    @Column(length = 10)
    private String mobileNumber;

    private Double latitude;

    private Double longitude;

    private LocalDateTime lastLocationAt;

    @CreationTimestamp
    private LocalDateTime createdAt;

    @UpdateTimestamp
    private LocalDateTime updatedAt;
}