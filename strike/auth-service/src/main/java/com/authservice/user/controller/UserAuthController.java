package com.authservice.user.controller;

import com.authservice.user.dto.AuthResponse;
import com.authservice.user.dto.SendOtpRequest;
import com.authservice.user.dto.VerifyOtpRequest;
import com.authservice.user.service.UserAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/user")
@RequiredArgsConstructor
public class UserAuthController {

    private final UserAuthService userAuthService;

    @PostMapping("/send-otp")
    public String sendOtp(
            @Valid
            @RequestBody
            SendOtpRequest request
    ) {

        userAuthService.sendOtp(request);

        return "OTP sent successfully";
    }

    @PostMapping("/verify-otp")
    public AuthResponse verifyOtp(
            @Valid
            @RequestBody
            VerifyOtpRequest request
    ) {

        return userAuthService.verifyOtp(request);
    }
}