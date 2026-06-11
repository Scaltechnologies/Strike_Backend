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
    private String token;
    private Long vendorId;
    private String hotelName;
    private String mobileNumber;
    private String email;
    private String address;
    private String status;
    private String message;
}