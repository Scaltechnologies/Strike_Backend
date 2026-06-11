package com.admin_service.auth.service;

import com.admin_service.auth.dto.*;
import com.admin_service.auth.entity.Admin;
import com.admin_service.auth.repository.AdminRepository;
import com.admin_service.auth.service.impl.AdminAuthServiceImpl;
import com.admin_service.config.AdminJwtUtil;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AdminAuthServiceImplTest {

    @Mock AdminRepository adminRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AdminJwtUtil jwtUtil;
    @Mock AdminRefreshTokenService refreshTokenService;

    @InjectMocks AdminAuthServiceImpl adminAuthService;

    // ── login ────────────────────────────────────────────────────────────────

    @Test
    void login_adminNotFound_throwsInvalidCredentials() {
        when(adminRepository.findByEmail("unknown@test.com")).thenReturn(Optional.empty());

        AdminLoginRequest req = new AdminLoginRequest();
        req.setEmail("unknown@test.com");
        req.setPassword("anything");

        assertThatThrownBy(() -> adminAuthService.login(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid credentials");
    }

    @Test
    void login_deactivatedAdmin_throwsDeactivated() {
        Admin inactive = Admin.builder()
                .id(1L).email("admin@test.com").password("hash")
                .active(false).role("ADMIN").build();
        when(adminRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(inactive));

        AdminLoginRequest req = new AdminLoginRequest();
        req.setEmail("admin@test.com");
        req.setPassword("password");

        assertThatThrownBy(() -> adminAuthService.login(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("deactivated");
    }

    @Test
    void login_wrongPassword_throwsInvalidCredentials() {
        Admin admin = Admin.builder()
                .id(1L).email("admin@test.com").password("hash")
                .active(true).role("ADMIN").build();
        when(adminRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("wrong", "hash")).thenReturn(false);

        AdminLoginRequest req = new AdminLoginRequest();
        req.setEmail("admin@test.com");
        req.setPassword("wrong");

        assertThatThrownBy(() -> adminAuthService.login(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid credentials");
    }

    @Test
    void login_validCredentials_returnsLoginResponseWithTokens() {
        Admin admin = Admin.builder()
                .id(1L).email("admin@test.com").name("Admin One").password("hash")
                .active(true).role("SUPER_ADMIN").build();
        when(adminRepository.findByEmail("admin@test.com")).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("correct", "hash")).thenReturn(true);
        when(adminRepository.save(any())).thenReturn(admin);
        when(jwtUtil.generateToken(1L, "admin@test.com", "SUPER_ADMIN")).thenReturn("admin.jwt");
        when(jwtUtil.getExpirationMs()).thenReturn(3600000L);
        when(refreshTokenService.create(1L)).thenReturn("admin-refresh-token");

        AdminLoginRequest req = new AdminLoginRequest();
        req.setEmail("admin@test.com");
        req.setPassword("correct");

        AdminLoginResponse response = adminAuthService.login(req);

        assertThat(response.getToken()).isEqualTo("admin.jwt");
        assertThat(response.getRefreshToken()).isEqualTo("admin-refresh-token");
        assertThat(response.getExpiresIn()).isEqualTo(3600L);
        assertThat(response.getRole()).isEqualTo("SUPER_ADMIN");
    }

    // ── register ─────────────────────────────────────────────────────────────

    @Test
    void register_duplicateEmail_throwsRuntimeException() {
        when(adminRepository.existsByEmail("dup@test.com")).thenReturn(true);

        AdminRegisterRequest req = new AdminRegisterRequest();
        req.setEmail("dup@test.com");
        req.setPassword("password");
        req.setName("Dup Admin");
        req.setRole("ADMIN");

        assertThatThrownBy(() -> adminAuthService.register(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already registered");
    }

    @Test
    void register_superAdminRole_preservedCorrectly() {
        when(adminRepository.existsByEmail("super@test.com")).thenReturn(false);
        when(passwordEncoder.encode("password")).thenReturn("hash");
        when(adminRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AdminRegisterRequest req = new AdminRegisterRequest();
        req.setEmail("super@test.com");
        req.setPassword("password");
        req.setName("Super Admin");
        req.setRole("SUPER_ADMIN");

        Admin result = adminAuthService.register(req);
        assertThat(result.getRole()).isEqualTo("SUPER_ADMIN");
    }

    @Test
    void register_unknownRole_defaultsToAdmin() {
        when(adminRepository.existsByEmail("new@test.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("hash");
        when(adminRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        AdminRegisterRequest req = new AdminRegisterRequest();
        req.setEmail("new@test.com");
        req.setPassword("pass");
        req.setName("New Admin");
        req.setRole("ADMIN");

        Admin result = adminAuthService.register(req);
        assertThat(result.getRole()).isEqualTo("ADMIN");
    }

    // ── changePassword ───────────────────────────────────────────────────────

    @Test
    void changePassword_wrongCurrentPassword_throwsRuntimeException() {
        Admin admin = Admin.builder().id(1L).password("current-hash").build();
        when(adminRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("wrong", "current-hash")).thenReturn(false);

        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword("wrong");
        req.setNewPassword("newpass");

        assertThatThrownBy(() -> adminAuthService.changePassword(1L, req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("incorrect");
    }

    @Test
    void changePassword_correct_updatesHash() {
        Admin admin = Admin.builder().id(1L).password("current-hash").build();
        when(adminRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(passwordEncoder.matches("correct", "current-hash")).thenReturn(true);
        when(passwordEncoder.encode("newpass")).thenReturn("new-hash");
        when(adminRepository.save(any())).thenReturn(admin);

        ChangePasswordRequest req = new ChangePasswordRequest();
        req.setCurrentPassword("correct");
        req.setNewPassword("newpass");

        adminAuthService.changePassword(1L, req);

        assertThat(admin.getPassword()).isEqualTo("new-hash");
        verify(adminRepository).save(admin);
    }

    // ── setActive ────────────────────────────────────────────────────────────

    @Test
    void setActive_false_deactivatesAdmin() {
        Admin admin = Admin.builder().id(1L).active(true).build();
        when(adminRepository.findById(1L)).thenReturn(Optional.of(admin));
        when(adminRepository.save(any())).thenReturn(admin);

        adminAuthService.setActive(1L, false);

        assertThat(admin.getActive()).isFalse();
    }

    @Test
    void setActive_notFound_throwsRuntimeException() {
        when(adminRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> adminAuthService.setActive(99L, true))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not found");
    }
}