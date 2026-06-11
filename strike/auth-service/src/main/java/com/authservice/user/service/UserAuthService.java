package com.authservice.user.service;

import com.authservice.user.dto.AuthResponse;
import com.authservice.user.dto.SendOtpRequest;
import com.authservice.user.dto.VerifyOtpRequest;

public interface UserAuthService {

    String sendOtp(SendOtpRequest request);

    AuthResponse verifyOtp(VerifyOtpRequest request);
}