package com.authservice.service;

import com.authservice.dto.RegisterVendorRequest;
import com.authservice.dto.VendorAuthResponse;
import com.authservice.entity.Vendor;
import com.authservice.repository.VendorRepository;
import com.authservice.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class VendorAuthService {

    private final VendorRepository vendorRepository;
    private final OtpService otpService;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final RestTemplate restTemplate;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    @Value("${services.vendor-url:http://localhost:8083}")
    private String vendorServiceUrl;

    @Value("${services.admin-url:http://localhost:8087}")
    private String adminServiceUrl;

    public void registerVendor(RegisterVendorRequest request) {
        boolean exists = vendorRepository.findByMobileNumber(request.getMobileNumber())
                .map(existing -> {
                    switch (existing.getStatus()) {
                        case "ACTIVE" -> throw new RuntimeException("Vendor already registered. Please login.");
                        case "REJECTED" -> throw new RuntimeException("Your registration was rejected. Please contact support.");
                        case "SUSPENDED" -> throw new RuntimeException("Your account is suspended. Please contact support.");
                    }
                    return true;
                })
                .orElse(false);

        if (!exists) {
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
        }

        otpService.generateOtp(request.getMobileNumber(), "VENDOR");
    }

    public void sendLoginOtp(String mobileNumber) {
        Vendor vendor = vendorRepository.findByMobileNumber(mobileNumber)
                .orElseThrow(() -> new RuntimeException("Vendor not registered"));

        if (!"ACTIVE".equals(vendor.getStatus())) {
            throw new RuntimeException("Vendor account not active. Current status: " + vendor.getStatus());
        }

        otpService.generateOtp(mobileNumber, "VENDOR");
    }

    @Transactional
    public VendorAuthResponse verifyOtp(String mobile, String otp) {
        if (!otpService.verifyOtp(mobile, otp)) {
            throw new RuntimeException("Invalid or expired OTP");
        }

        Vendor vendor = vendorRepository.findByMobileNumber(mobile)
                .orElseThrow(() -> new RuntimeException("Vendor not found"));

        if ("PENDING".equals(vendor.getStatus())) {
            vendor.setStatus("VERIFIED");
            vendorRepository.save(vendor);
            notifyAdminService(vendor);
            return VendorAuthResponse.builder()
                    .vendorId(vendor.getId())
                    .hotelName(vendor.getHotelName())
                    .mobileNumber(vendor.getMobileNumber())
                    .email(vendor.getEmail())
                    .status("VERIFIED")
                    .message("Registration verified. Pending admin approval.")
                    .build();
        }

        if ("VERIFIED".equals(vendor.getStatus())) {
            return VendorAuthResponse.builder()
                    .vendorId(vendor.getId())
                    .status("VERIFIED")
                    .message("Your registration is pending admin approval.")
                    .build();
        }

        if (!"ACTIVE".equals(vendor.getStatus())) {
            throw new RuntimeException("Vendor account is " + vendor.getStatus() + ". Access denied.");
        }

        String accessToken = jwtUtil.generateToken(vendor.getId(), mobile, "VENDOR");
        String refreshToken = refreshTokenService.createForVendor(vendor.getId());

        return VendorAuthResponse.builder()
                .token(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtExpirationMs / 1000)
                .vendorId(vendor.getId())
                .hotelName(vendor.getHotelName())
                .mobileNumber(vendor.getMobileNumber())
                .email(vendor.getEmail())
                .address(vendor.getAddress())
                .status(vendor.getStatus())
                .build();
    }

    public void approveVendor(Long vendorId) {
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new RuntimeException("Vendor not found: " + vendorId));
        vendor.setStatus("ACTIVE");
        vendorRepository.save(vendor);
        syncVendorProfile(vendor);
    }

    public void rejectVendor(Long vendorId) {
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new RuntimeException("Vendor not found: " + vendorId));
        vendor.setStatus("REJECTED");
        vendorRepository.save(vendor);
    }

    public void suspendVendor(Long vendorId) {
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new RuntimeException("Vendor not found: " + vendorId));
        vendor.setStatus("SUSPENDED");
        vendorRepository.save(vendor);
    }

    public void reactivateVendor(Long vendorId) {
        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new RuntimeException("Vendor not found: " + vendorId));
        if (!"SUSPENDED".equals(vendor.getStatus())) {
            throw new RuntimeException("Vendor is not suspended. Current status: " + vendor.getStatus());
        }
        vendor.setStatus("ACTIVE");
        vendorRepository.save(vendor);
        syncVendorProfile(vendor);
    }

    private void syncVendorProfile(Vendor vendor) {
        try {
            java.util.Map<String, Object> body = new java.util.HashMap<>();
            body.put("vendorId", vendor.getId());
            body.put("shopName", vendor.getHotelName() != null ? vendor.getHotelName() : "");
            body.put("mobile", vendor.getMobileNumber() != null ? vendor.getMobileNumber() : "");
            body.put("address", vendor.getAddress() != null ? vendor.getAddress() : "");
            body.put("email", vendor.getEmail() != null ? vendor.getEmail() : "");
            if (vendor.getLatitude() != null) body.put("latitude", vendor.getLatitude());
            if (vendor.getLongitude() != null) body.put("longitude", vendor.getLongitude());
            restTemplate.postForObject(
                    vendorServiceUrl + "/internal/vendors/" + vendor.getId() + "/init",
                    body,
                    Object.class);
        } catch (Exception e) {
            log.warn("Could not sync vendor profile to vendor-service: {}", e.getMessage());
        }
    }

    private void notifyAdminService(Vendor vendor) {
        try {
            restTemplate.postForObject(
                    adminServiceUrl + "/internal/admin/vendors",
                    Map.of(
                            "vendorId", vendor.getId(),
                            "hotelName", vendor.getHotelName() != null ? vendor.getHotelName() : "",
                            "mobileNumber", vendor.getMobileNumber() != null ? vendor.getMobileNumber() : "",
                            "email", vendor.getEmail() != null ? vendor.getEmail() : "",
                            "status", "PENDING"
                    ),
                    Object.class);
        } catch (Exception e) {
            log.warn("Could not notify admin-service of new vendor registration: {}", e.getMessage());
        }
    }
}