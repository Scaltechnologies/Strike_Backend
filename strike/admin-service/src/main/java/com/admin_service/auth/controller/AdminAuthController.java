package com.admin_service.auth.controller;

import com.admin_service.auth.dto.*;
import com.admin_service.auth.entity.Admin;
import com.admin_service.auth.service.AdminAuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    // ── Public ───────────────────────────────────────────────────────────────

    @PostMapping("/login")
    public ResponseEntity<AdminLoginResponse> login(@Valid @RequestBody AdminLoginRequest request) {
        return ResponseEntity.ok(adminAuthService.login(request));
    }

    @PostMapping("/setup")
    public ResponseEntity<Map<String, Object>> setup(@Valid @RequestBody AdminRegisterRequest request) {
        Admin admin = adminAuthService.setup(request);
        return ResponseEntity.ok(Map.of(
                "message", "Super admin created successfully",
                "adminId", admin.getId(),
                "email", admin.getEmail()
        ));
    }

    // ── Protected (require admin JWT) ────────────────────────────────────────

    @GetMapping("/me")
    public ResponseEntity<Map<String, Object>> me() {
        Long adminId = currentAdminId();
        Admin admin = adminAuthService.getById(adminId);
        return ResponseEntity.ok(Map.of(
                "id", admin.getId(),
                "email", admin.getEmail(),
                "name", admin.getName(),
                "role", admin.getRole(),
                "active", admin.getActive(),
                "lastLoginAt", admin.getLastLoginAt() != null ? admin.getLastLoginAt().toString() : "",
                "createdAt", admin.getCreatedAt().toString()
        ));
    }

    @PatchMapping("/change-password")
    public ResponseEntity<Map<String, String>> changePassword(
            @Valid @RequestBody ChangePasswordRequest request) {
        adminAuthService.changePassword(currentAdminId(), request);
        return ResponseEntity.ok(Map.of("message", "Password changed successfully"));
    }

    @PostMapping("/register")
    public ResponseEntity<Map<String, Object>> register(
            @Valid @RequestBody AdminRegisterRequest request) {
        requireSuperAdmin();
        Admin admin = adminAuthService.register(request);
        return ResponseEntity.ok(Map.of(
                "message", "Admin registered successfully",
                "adminId", admin.getId(),
                "email", admin.getEmail(),
                "role", admin.getRole()
        ));
    }

    @GetMapping("/admins")
    public ResponseEntity<Map<String, Object>> listAdmins() {
        requireSuperAdmin();
        return ResponseEntity.ok(Map.of("admins", adminAuthService.getAllAdmins()));
    }

    @PatchMapping("/admins/{adminId}/deactivate")
    public ResponseEntity<Map<String, String>> deactivateAdmin(@PathVariable Long adminId) {
        requireSuperAdmin();
        if (adminId.equals(currentAdminId())) {
            throw new RuntimeException("Cannot deactivate your own account");
        }
        adminAuthService.setActive(adminId, false);
        return ResponseEntity.ok(Map.of("message", "Admin deactivated"));
    }

    @PatchMapping("/admins/{adminId}/activate")
    public ResponseEntity<Map<String, String>> activateAdmin(@PathVariable Long adminId) {
        requireSuperAdmin();
        adminAuthService.setActive(adminId, true);
        return ResponseEntity.ok(Map.of("message", "Admin activated"));
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Long currentAdminId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return (Long) auth.getCredentials();
    }

    private void requireSuperAdmin() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        boolean isSuperAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SUPER_ADMIN"));
        if (!isSuperAdmin) {
            throw new RuntimeException("Access denied: SUPER_ADMIN role required");
        }
    }
}