package com.authservice.dto;

import lombok.Data;

@Data
public class RegisterVendorRequest {

    private String hotelName;
    private String address;
    private String mobileNumber;
    private String email;
    private Double latitude;
    private Double longitude;
}