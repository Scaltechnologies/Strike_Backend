package com.authservice.controller;

import com.authservice.dto.RegisterVendorRequest;
import com.authservice.dto.VendorAuthResponse;
import com.authservice.dto.VerifyOtpRequest;
import com.authservice.dto.VendorLoginRequest;
import com.authservice.service.VendorAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth/vendor")
@RequiredArgsConstructor
public class VendorAuthController {

    private final VendorAuthService vendorAuthService;

    @PostMapping("/register")
    public ResponseEntity<?> registerVendor(@Valid @RequestBody RegisterVendorRequest request) {
        try {
            vendorAuthService.registerVendor(request);
            return ResponseEntity.ok("Registration successful. Check server console for OTP.");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<?> loginVendor(@Valid @RequestBody VendorLoginRequest request) {
        try {
            vendorAuthService.sendLoginOtp(request.getMobileNumber());
            return ResponseEntity.ok("OTP sent. Check server console for OTP.");
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }
    }

    @PostMapping("/verify")
    public ResponseEntity<VendorAuthResponse> verifyOtp(@Valid @RequestBody VerifyOtpRequest request) {
        try {
            VendorAuthResponse response = vendorAuthService.verifyOtp(
                    request.getMobileNumber(), request.getOtp());
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
    }
}