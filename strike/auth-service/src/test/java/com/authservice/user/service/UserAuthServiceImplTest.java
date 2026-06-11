package com.authservice.user.service;

import com.authservice.security.JwtUtil;
import com.authservice.service.OtpService;
import com.authservice.service.RefreshTokenService;
import com.authservice.user.dto.AuthResponse;
import com.authservice.user.dto.SendOtpRequest;
import com.authservice.user.dto.VerifyOtpRequest;
import com.authservice.user.entity.UserAuth;
import com.authservice.user.repository.UserAuthRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAuthServiceImplTest {

    @Mock UserAuthRepository userAuthRepository;
    @Mock OtpService otpService;
    @Mock JwtUtil jwtUtil;
    @Mock RefreshTokenService refreshTokenService;

    @InjectMocks UserAuthServiceImpl userAuthService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(userAuthService, "jwtExpirationMs", 3600000L);
    }

    // ── sendOtp ─────────────────────────────────────────────────────────────

    @Test
    void sendOtp_bannedUser_throwsRuntimeException() {
        UserAuth banned = UserAuth.builder()
                .mobileNumber("9999999999").banned(true).verified(true).build();
        when(userAuthRepository.findByMobileNumber("9999999999"))
                .thenReturn(Optional.of(banned));

        SendOtpRequest req = new SendOtpRequest();
        req.setMobileNumber("9999999999");

        assertThatThrownBy(() -> userAuthService.sendOtp(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("banned");
        verify(otpService, never()).generateOtp(any());
    }

    @Test
    void sendOtp_unknownNumber_generatesOtpSuccessfully() {
        when(userAuthRepository.findByMobileNumber("9876543210")).thenReturn(Optional.empty());
        when(otpService.generateOtp("9876543210")).thenReturn("123456");

        SendOtpRequest req = new SendOtpRequest();
        req.setMobileNumber("9876543210");

        String result = userAuthService.sendOtp(req);
        assertThat(result).isEqualTo("OTP Sent Successfully");
        verify(otpService).generateOtp("9876543210");
    }

    @Test
    void sendOtp_existingActiveUser_generatesOtp() {
        UserAuth existing = UserAuth.builder()
                .mobileNumber("9876543210").banned(false).verified(true).build();
        when(userAuthRepository.findByMobileNumber("9876543210"))
                .thenReturn(Optional.of(existing));

        SendOtpRequest req = new SendOtpRequest();
        req.setMobileNumber("9876543210");

        userAuthService.sendOtp(req);
        verify(otpService).generateOtp("9876543210");
    }

    // ── verifyOtp ────────────────────────────────────────────────────────────

    @Test
    void verifyOtp_invalidOtp_throwsRuntimeException() {
        when(otpService.verifyOtp("9876543210", "000000")).thenReturn(false);

        VerifyOtpRequest req = new VerifyOtpRequest();
        req.setMobileNumber("9876543210");
        req.setOtp("000000");

        assertThatThrownBy(() -> userAuthService.verifyOtp(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid OTP");
    }

    @Test
    void verifyOtp_bannedUser_throwsRuntimeException() {
        when(otpService.verifyOtp("9876543210", "123456")).thenReturn(true);
        UserAuth banned = UserAuth.builder()
                .id(1L).mobileNumber("9876543210").banned(true).verified(true).build();
        when(userAuthRepository.findByMobileNumber("9876543210"))
                .thenReturn(Optional.of(banned));

        VerifyOtpRequest req = new VerifyOtpRequest();
        req.setMobileNumber("9876543210");
        req.setOtp("123456");

        assertThatThrownBy(() -> userAuthService.verifyOtp(req))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("banned");
    }

    @Test
    void verifyOtp_newUser_createsAccountAndReturnsTokens() {
        when(otpService.verifyOtp("9876543210", "123456")).thenReturn(true);
        when(userAuthRepository.findByMobileNumber("9876543210")).thenReturn(Optional.empty());

        UserAuth created = UserAuth.builder()
                .id(1L).mobileNumber("9876543210").verified(true).banned(false).build();
        when(userAuthRepository.save(any(UserAuth.class))).thenReturn(created);
        when(jwtUtil.generateToken(1L, "9876543210", "USER")).thenReturn("access.token.here");
        when(refreshTokenService.createForUser(1L)).thenReturn("refresh-uuid");

        VerifyOtpRequest req = new VerifyOtpRequest();
        req.setMobileNumber("9876543210");
        req.setOtp("123456");

        AuthResponse response = userAuthService.verifyOtp(req);

        assertThat(response.isNewUser()).isTrue();
        assertThat(response.getToken()).isEqualTo("access.token.here");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-uuid");
        assertThat(response.getExpiresIn()).isEqualTo(3600L);
        assertThat(response.getMessage()).contains("Registered");
        verify(userAuthRepository).save(any(UserAuth.class));
    }

    @Test
    void verifyOtp_existingUser_logsInAndReturnsTokens() {
        when(otpService.verifyOtp("9876543210", "123456")).thenReturn(true);
        UserAuth existing = UserAuth.builder()
                .id(5L).mobileNumber("9876543210").verified(true).banned(false).build();
        when(userAuthRepository.findByMobileNumber("9876543210"))
                .thenReturn(Optional.of(existing));
        when(userAuthRepository.save(any(UserAuth.class))).thenReturn(existing);
        when(jwtUtil.generateToken(5L, "9876543210", "USER")).thenReturn("access.token");
        when(refreshTokenService.createForUser(5L)).thenReturn("refresh-uuid");

        VerifyOtpRequest req = new VerifyOtpRequest();
        req.setMobileNumber("9876543210");
        req.setOtp("123456");

        AuthResponse response = userAuthService.verifyOtp(req);

        assertThat(response.isNewUser()).isFalse();
        assertThat(response.getMessage()).contains("Login");
    }
}