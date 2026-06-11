package com.user_service.profile.dto;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class UserProfileResponse {
    private Long userId;
    private String name;
    private String email;
    private String mobileNumber;
    private String profilePicUrl;
    private Double latitude;
    private Double longitude;
    private LocalDateTime lastLocationAt;
    private LocalDateTime createdAt;
}