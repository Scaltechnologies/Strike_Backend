package com.authservice.service;

import com.authservice.entity.OtpCode;
import com.authservice.repository.OtpRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpService {

    private static final int OTP_COOLDOWN_SECONDS = 60;
    private static final int OTP_MAX_PER_HOUR = 5;

    private final OtpRepository otpRepository;
    private final RestTemplate restTemplate;

    @Value("${services.notification-url:http://localhost:8088}")
    private String notificationServiceUrl;

    public String generateOtp(String mobile) {
        return generateOtp(mobile, "USER");
    }

    public String generateOtp(String mobile, String recipientType) {
        LocalDateTime now = LocalDateTime.now();

        // Cooldown: reject if an OTP was sent less than 60 seconds ago
        otpRepository.findTopByMobileOrderByCreatedAtDesc(mobile).ifPresent(last -> {
            long secondsElapsed = java.time.Duration.between(last.getCreatedAt(), now).getSeconds();
            if (secondsElapsed < OTP_COOLDOWN_SECONDS) {
                long waitSeconds = OTP_COOLDOWN_SECONDS - secondsElapsed;
                throw new RuntimeException("Please wait " + waitSeconds + " second(s) before requesting another OTP.");
            }
        });

        // Hourly cap: reject if 5 or more OTPs were sent in the last hour
        int recentCount = otpRepository.countByMobileAndCreatedAtAfter(mobile, now.minusHours(1));
        if (recentCount >= OTP_MAX_PER_HOUR) {
            throw new RuntimeException("Too many OTP requests. Please try again after an hour.");
        }

        String otp = String.valueOf(
                (int)((Math.random()*900000)+100000)
        );

        OtpCode code = OtpCode.builder()
                .mobile(mobile)
                .otpCode(otp)
                .verified(false)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        otpRepository.save(code);

        try {
            restTemplate.postForObject(
                    notificationServiceUrl + "/internal/notify/otp",
                    Map.of("mobile", mobile, "otp", otp, "recipientType", recipientType),
                    String.class);
        } catch (Exception e) {
            log.warn("Notification service unavailable — OTP for {} = {}", mobile, otp);
        }

        return otp;
    }

    public boolean verifyOtp(
            String mobile,
            String otp
    ){

        OtpCode code = otpRepository
                .findTopByMobileOrderByCreatedAtDesc(
                        mobile
                )
                .orElseThrow();

        if(code.getExpiresAt()
                .isBefore(LocalDateTime.now()))
            return false;

        if(!code.getOtpCode().equals(otp))
            return false;

        code.setVerified(true);

        otpRepository.save(code);

        return true;
    }
}