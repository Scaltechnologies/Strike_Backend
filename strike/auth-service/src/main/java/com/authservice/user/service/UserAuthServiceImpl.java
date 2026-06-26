package com.authservice.user.service;

import com.authservice.client.UserServiceClient;
import com.authservice.security.JwtUtil;
import com.authservice.service.OtpService;
import com.authservice.service.RefreshTokenService;
import com.authservice.user.dto.AuthResponse;
import com.authservice.user.dto.SendOtpRequest;
import com.authservice.user.dto.VerifyOtpRequest;
import com.authservice.user.entity.UserAuth;
import com.authservice.user.repository.UserAuthRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserAuthServiceImpl implements UserAuthService {

    private final UserAuthRepository userAuthRepository;
    private final OtpService otpService;
    private final JwtUtil jwtUtil;
    private final RefreshTokenService refreshTokenService;
    private final UserServiceClient userServiceClient;

    @Value("${jwt.expiration}")
    private long jwtExpirationMs;

    @Override
    public String sendOtp(SendOtpRequest request) {
        userAuthRepository.findByMobileNumber(request.getMobileNumber())
                .ifPresent(user -> {
                    if (Boolean.TRUE.equals(user.getBanned())) {
                        throw new RuntimeException("Account is banned. Please contact support.");
                    }
                });
        otpService.generateOtp(request.getMobileNumber());
        return "OTP Sent Successfully";
    }

    @Override
    @Transactional
    public AuthResponse verifyOtp(VerifyOtpRequest request) {
        boolean valid = otpService.verifyOtp(request.getMobileNumber(), request.getOtp());
        if (!valid) {
            throw new RuntimeException("Invalid OTP");
        }

        UserAuth user = userAuthRepository.findByMobileNumber(request.getMobileNumber()).orElse(null);
        boolean newUser = false;

        if (user != null && Boolean.TRUE.equals(user.getBanned())) {
            throw new RuntimeException("Account is banned. Please contact support.");
        }

        if (user == null) {
            user = UserAuth.builder()
                    .mobileNumber(request.getMobileNumber())
                    .verified(true)
                    .build();
            user = userAuthRepository.save(user);
            newUser = true;
        } else {
            user.setVerified(true);
            user = userAuthRepository.save(user);
        }

        String accessToken = jwtUtil.generateToken(user.getId(), user.getMobileNumber(), "USER");
        String refreshToken = refreshTokenService.createForUser(user.getId());

        // Ensure user_profiles row exists for both new and returning users.
        // This is idempotent — safe to call on every login.
        // A missing profile (the root-cause bug) is also repaired here on next login.
        userServiceClient.ensureProfile(user.getId(), user.getMobileNumber());

        return AuthResponse.builder()
                .userId(user.getId())
                .mobileNumber(user.getMobileNumber())
                .token(accessToken)
                .refreshToken(refreshToken)
                .expiresIn(jwtExpirationMs / 1000)
                .newUser(newUser)
                .message(newUser ? "User Registered Successfully" : "Login Successful")
                .build();
    }
}