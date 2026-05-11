
package com.authservice.controller;

import com.authservice.dto.RegisterVendorRequest;
import com.authservice.dto.VerifyOtpRequest;
import com.authservice.dto.VendorLoginRequest;
import com.authservice.service.VendorAuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth/vendor")
@RequiredArgsConstructor
public class VendorAuthController {

    private final VendorAuthService vendorAuthService;

    // Register vendor and send OTP
    @PostMapping("/register")
    public ResponseEntity<?> registerVendor(@RequestBody RegisterVendorRequest request) {

        vendorAuthService.registerVendor(request);

        return ResponseEntity.ok("OTP sent successfully for registration");
    }

    // Login vendor and send OTP
    @PostMapping("/login")
    public ResponseEntity<?> loginVendor(@RequestBody VendorLoginRequest request) {

        vendorAuthService.sendLoginOtp(request.getMobile());

        return ResponseEntity.ok("OTP sent successfully for login");
    }

    // Verify OTP and generate JWT
    @PostMapping("/verify")
    public ResponseEntity<?> verifyOtp(@RequestBody VerifyOtpRequest request) {

        String token = vendorAuthService
                .verifyOtp(request.getMobileNumber(), request.getOtp());

        return ResponseEntity.ok(token);
    }
}
