package com.authservice.user.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private Long userId;
    private String mobileNumber;
    private String token;         // access token (kept for backward compatibility)
    private String refreshToken;
    private long expiresIn;       // access token TTL in seconds
    private Boolean newUser;
    private String message;
}