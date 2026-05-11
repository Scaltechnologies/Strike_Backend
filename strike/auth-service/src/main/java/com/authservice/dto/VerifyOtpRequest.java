package com.authservice.dto;

import lombok.Data;

@Data
public class VerifyOtpRequest {

    private String mobileNumber;
    private String otp;
}