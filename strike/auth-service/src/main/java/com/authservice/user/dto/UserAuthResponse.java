package com.authservice.user.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserAuthResponse {
    private Long id;
    private String mobileNumber;
    private Boolean verified;
    private Boolean banned;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}