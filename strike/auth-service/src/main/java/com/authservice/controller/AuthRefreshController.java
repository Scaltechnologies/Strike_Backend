package com.authservice.controller;

import com.authservice.dto.RefreshRequest;
import com.authservice.entity.RefreshToken;
import com.authservice.entity.Vendor;
import com.authservice.repository.VendorRepository;
import com.authservice.security.JwtUtil;
import com.authservice.service.RefreshTokenService;
import com.authservice.user.entity.UserAuth;
import com.authservice.user.repository.UserAuthRepository;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthRefreshController {

    private final RefreshTokenService refreshTokenService;
    private final JwtUtil jwtUtil;
    private final UserAuthRepository userAuthRepository;
    private final VendorRepository vendorRepository;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    /**
     * Issue a new access token using a valid refresh token.
     * The refresh token is rotated on each call (old one is revoked, new one issued).
     */
    @PostMapping("/refresh")
    public ResponseEntity<Map<String, Object>> refresh(@Valid @RequestBody RefreshRequest request) {
        RefreshToken rt = refreshTokenService.validate(request.getRefreshToken());

        String role = rt.getRole();
        Long userId = rt.getUserId();
        Long vendorId = rt.getVendorId();

        String newAccessToken;

        if ("USER".equals(role)) {
            UserAuth user = userAuthRepository.findById(userId)
                    .orElseThrow(() -> new RuntimeException("User not found"));
            if (Boolean.TRUE.equals(user.getBanned())) {
                refreshTokenService.revoke(request.getRefreshToken());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Account is banned. Please contact support."));
            }
            newAccessToken = jwtUtil.generateToken(user.getId(), user.getMobileNumber(), "USER");

        } else {
            Vendor vendor = vendorRepository.findById(vendorId)
                    .orElseThrow(() -> new RuntimeException("Vendor not found"));
            if (!"ACTIVE".equals(vendor.getStatus())) {
                refreshTokenService.revoke(request.getRefreshToken());
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Vendor account is " + vendor.getStatus() + ". Access denied."));
            }
            newAccessToken = jwtUtil.generateToken(vendor.getId(), vendor.getMobileNumber(), "VENDOR");
        }

        // Rotate: revoke old, issue new refresh token
        String newRefreshToken = "USER".equals(role)
                ? refreshTokenService.createForUser(userId)
                : refreshTokenService.createForVendor(vendorId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("accessToken", newAccessToken);
        response.put("token", newAccessToken);          // backward compat alias
        response.put("refreshToken", newRefreshToken);
        response.put("expiresIn", jwtExpirationMs / 1000);

        return ResponseEntity.ok(response);
    }

    /**
     * Revoke the refresh token (logout). The access token will expire naturally.
     */
    @PostMapping("/logout")
    public ResponseEntity<String> logout(@Valid @RequestBody RefreshRequest request) {
        refreshTokenService.revoke(request.getRefreshToken());
        return ResponseEntity.ok("Logged out successfully");
    }
}