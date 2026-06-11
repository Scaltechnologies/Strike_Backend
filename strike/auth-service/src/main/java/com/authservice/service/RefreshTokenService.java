package com.authservice.service;

import com.authservice.entity.RefreshToken;
import com.authservice.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpirationMs;

    @Transactional
    public String createForUser(Long userId) {
        refreshTokenRepository.deleteByUserId(userId);
        return save(RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .userId(userId)
                .role("USER")
                .expiresAt(LocalDateTime.now().plusSeconds(refreshExpirationMs / 1000))
                .createdAt(LocalDateTime.now())
                .build());
    }

    @Transactional
    public String createForVendor(Long vendorId) {
        refreshTokenRepository.deleteByVendorId(vendorId);
        return save(RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .vendorId(vendorId)
                .role("VENDOR")
                .expiresAt(LocalDateTime.now().plusSeconds(refreshExpirationMs / 1000))
                .createdAt(LocalDateTime.now())
                .build());
    }

    public RefreshToken validate(String token) {
        RefreshToken rt = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));
        if (rt.getExpiresAt().isBefore(LocalDateTime.now())) {
            refreshTokenRepository.deleteByToken(token);
            throw new RuntimeException("Refresh token expired. Please login again.");
        }
        return rt;
    }

    @Transactional
    public void revoke(String token) {
        refreshTokenRepository.deleteByToken(token);
    }

    private String save(RefreshToken rt) {
        return refreshTokenRepository.save(rt).getToken();
    }
}