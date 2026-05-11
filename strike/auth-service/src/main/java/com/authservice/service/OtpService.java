package com.authservice.service;

import com.authservice.entity.OtpCode;
import com.authservice.repository.OtpRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpRepository otpRepository;

    public String generateOtp(String mobile){

        String otp = String.valueOf((int)((Math.random()*900000)+100000));

        OtpCode code = OtpCode.builder()
                .id(UUID.randomUUID())
                .mobile(mobile)
                .otpCode(otp)
                .verified(false)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();

        otpRepository.save(code);

        // SMS integration later
        System.out.println("OTP for " + mobile + " = " + otp);

        return otp;
    }

    public boolean verifyOtp(String mobile,String otp){

        OtpCode code = otpRepository
                .findTopByMobileOrderByCreatedAtDesc(mobile)
                .orElseThrow();

        if(code.getExpiresAt().isBefore(LocalDateTime.now()))
            return false;

        if(!code.getOtpCode().equals(otp))
            return false;

        code.setVerified(true);
        otpRepository.save(code);

        return true;
    }
}