package com.authservice.service;

import com.authservice.dto.RegisterVendorRequest;
import com.authservice.entity.Vendor;
import com.authservice.repository.VendorRepository;
import com.authservice.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class VendorAuthService {

    private final VendorRepository vendorRepository;
    private final OtpService otpService;
    private final JwtUtil jwtUtil;

    // Register vendor and send OTP
    public void registerVendor(RegisterVendorRequest request) {

        Vendor vendor = Vendor.builder()
                .hotelName(request.getHotelName())
                .address(request.getAddress())
                .mobileNumber(request.getMobileNumber())
                .email(request.getEmail())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .status("PENDING")
                .createdAt(LocalDateTime.now())
                .build();

        vendorRepository.save(vendor);

        otpService.generateOtp(request.getMobileNumber());
    }

    // Login vendor -> send OTP only if vendor exists
    public void sendLoginOtp(String mobileNumber) {

        Vendor vendor = vendorRepository
                .findByMobileNumber(mobileNumber)
                .orElseThrow(() ->
                        new RuntimeException("Vendor not registered")
                );

        if (!"ACTIVE".equals(vendor.getStatus())) {
            throw new RuntimeException("Vendor account not active");
        }

        otpService.generateOtp(mobileNumber);
    }

    // Verify OTP and generate JWT
    public String verifyOtp(String mobile, String otp) {

        boolean valid = otpService.verifyOtp(mobile, otp);

        if (!valid) {
            throw new RuntimeException("Invalid OTP");
        }

        Vendor vendor = vendorRepository
                .findByMobileNumber(mobile)
                .orElseThrow(() ->
                        new RuntimeException("Vendor not found")
                );

        // If vendor just registered activate it
        if ("PENDING".equals(vendor.getStatus())) {

            vendor.setStatus("ACTIVE");

            vendorRepository.save(vendor);
        }

        return jwtUtil.generateToken(
                vendor.getId(),
                mobile
        );
    }
}