package com.admin_service.auth.service;

import com.admin_service.auth.entity.AdminRefreshToken;
import com.admin_service.auth.repository.AdminRefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminRefreshTokenService {

    private final AdminRefreshTokenRepository repository;

    @Value("${jwt.refresh-expiration}")
    private long refreshExpirationMs;

    @Transactional
    public String create(Long adminId) {
        repository.deleteByAdminId(adminId);
        AdminRefreshToken token = AdminRefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .adminId(adminId)
                .createdAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusSeconds(refreshExpirationMs / 1000))
                .build();
        return repository.save(token).getToken();
    }

    public AdminRefreshToken validate(String token) {
        AdminRefreshToken rt = repository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));
        if (rt.getExpiresAt().isBefore(LocalDateTime.now())) {
            repository.deleteByToken(token);
            throw new RuntimeException("Refresh token expired. Please log in again.");
        }
        return rt;
    }

    @Transactional
    public void revoke(String token) {
        repository.deleteByToken(token);
    }
}