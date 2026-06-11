package com.authservice.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VendorAuthResponse {
    private String token;         // access token (kept for backward compatibility)
    private String refreshToken;
    private long expiresIn;       // access token TTL in seconds
    private Long vendorId;
    private String hotelName;
    private String mobileNumber;
    private String email;
    private String address;
    private String status;
    private String message;
}