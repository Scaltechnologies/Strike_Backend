package com.authservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "otp_codes", schema = "auth_service")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpCode {

    @Id
    private UUID id;

    private String mobile;

    @Column(name = "otp_code")
    private String otpCode;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    private Boolean verified;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}