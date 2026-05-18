package com.authservice.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "otp_codes")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OtpCode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String mobile;

    @Column(name = "otp_code")
    private String otpCode;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    private Boolean verified;

    @Column(name = "created_at")
    private LocalDateTime createdAt;
}