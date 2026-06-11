package com.admin_service.auth.service.impl;

import com.admin_service.auth.dto.*;
import com.admin_service.auth.entity.Admin;
import com.admin_service.auth.repository.AdminRepository;
import com.admin_service.auth.service.AdminAuthService;
import com.admin_service.auth.service.AdminRefreshTokenService;
import com.admin_service.config.AdminJwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminAuthServiceImpl implements AdminAuthService, ApplicationRunner {

    private final AdminRepository adminRepository;
    private final PasswordEncoder passwordEncoder;
    private final AdminJwtUtil jwtUtil;
    private final AdminRefreshTokenService refreshTokenService;

    // Seeds the default super-admin on first startup
    @Override
    public void run(ApplicationArguments args) {
        if (adminRepository.count() == 0) {
            Admin superAdmin = Admin.builder()
                    .email("admin@strike.com")
                    .password(passwordEncoder.encode("Admin@123"))
                    .name("Strike Super Admin")
                    .role("SUPER_ADMIN")
                    .active(true)
                    .build();
            adminRepository.save(superAdmin);
            log.info("Default super admin seeded → email: admin@strike.com  password: Admin@123");
        }
    }

    @Override
    @Transactional
    public AdminLoginResponse login(AdminLoginRequest request) {
        Admin admin = adminRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!Boolean.TRUE.equals(admin.getActive())) {
            throw new RuntimeException("Account is deactivated. Contact the platform owner.");
        }
        if (!passwordEncoder.matches(request.getPassword(), admin.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        admin.setLastLoginAt(LocalDateTime.now());
        adminRepository.save(admin);

        String token = jwtUtil.generateToken(admin.getId(), admin.getEmail(), admin.getRole());
        String refreshToken = refreshTokenService.create(admin.getId());
        return AdminLoginResponse.builder()
                .adminId(admin.getId())
                .email(admin.getEmail())
                .name(admin.getName())
                .role(admin.getRole())
                .token(token)
                .refreshToken(refreshToken)
                .expiresIn(jwtUtil.getExpirationMs() / 1000)
                .build();
    }

    @Override
    public Admin register(AdminRegisterRequest request) {
        if (adminRepository.existsByEmail(request.getEmail())) {
            throw new RuntimeException("Email already registered: " + request.getEmail());
        }
        String role = "SUPER_ADMIN".equals(request.getRole()) ? "SUPER_ADMIN" : "ADMIN";
        Admin admin = Admin.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .role(role)
                .active(true)
                .build();
        return adminRepository.save(admin);
    }

    @Override
    public Admin setup(AdminRegisterRequest request) {
        if (adminRepository.count() > 0) {
            throw new RuntimeException("Setup already done. Use /api/admin/auth/login.");
        }
        Admin admin = Admin.builder()
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .name(request.getName())
                .role("SUPER_ADMIN")
                .active(true)
                .build();
        return adminRepository.save(admin);
    }

    @Override
    public void changePassword(Long adminId, ChangePasswordRequest request) {
        Admin admin = findById(adminId);
        if (!passwordEncoder.matches(request.getCurrentPassword(), admin.getPassword())) {
            throw new RuntimeException("Current password is incorrect");
        }
        admin.setPassword(passwordEncoder.encode(request.getNewPassword()));
        adminRepository.save(admin);
    }

    @Override
    public Admin getById(Long adminId) {
        return findById(adminId);
    }

    @Override
    public List<Admin> getAllAdmins() {
        return adminRepository.findAll();
    }

    @Override
    public void setActive(Long adminId, boolean active) {
        Admin admin = findById(adminId);
        admin.setActive(active);
        adminRepository.save(admin);
    }

    private Admin findById(Long adminId) {
        return adminRepository.findById(adminId)
                .orElseThrow(() -> new RuntimeException("Admin not found: " + adminId));
    }
}