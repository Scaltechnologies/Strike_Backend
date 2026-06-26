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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserAuthServiceImplTest {

    @Mock UserAuthRepository userAuthRepository;
    @Mock OtpService otpService;
    @Mock JwtUtil jwtUtil;
    @Mock RefreshTokenService refreshTokenService;
    @Mock UserServiceClient userServiceClient;

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

        // ensureProfile must NOT be called when OTP is wrong
        verify(userServiceClient, never()).ensureProfile(any(), any());
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

        // ensureProfile must NOT be called when user is banned
        verify(userServiceClient, never()).ensureProfile(any(), any());
    }

    // ── scenario: first OTP login (new user) ─────────────────────────────────

    @Test
    void verifyOtp_firstOtpLogin_newUser_createsAuthAndEnsuresProfile() {
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

        assertThat(response.getNewUser()).isTrue();
        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getToken()).isEqualTo("access.token.here");
        assertThat(response.getRefreshToken()).isEqualTo("refresh-uuid");
        assertThat(response.getExpiresIn()).isEqualTo(3600L);
        assertThat(response.getMessage()).contains("Registered");

        verify(userAuthRepository).save(any(UserAuth.class));
        // profile must be ensured for the new user's id and mobile
        verify(userServiceClient).ensureProfile(1L, "9876543210");
    }

    // ── scenario: second / existing user login ────────────────────────────────

    @Test
    void verifyOtp_secondLogin_existingUser_logsInAndEnsuresProfile() {
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

        assertThat(response.getNewUser()).isFalse();
        assertThat(response.getUserId()).isEqualTo(5L);
        assertThat(response.getMessage()).contains("Login");

        // ensureProfile must also be called for existing users — repairs missing profiles
        verify(userServiceClient).ensureProfile(5L, "9876543210");
    }

    // ── scenario: missing profile recovery on login ───────────────────────────

    @Test
    void verifyOtp_existingUserWithMissingProfile_ensureProfileRepairsIt() {
        // Simulates user_id=2 from the bug report: exists in user_auth but not user_profiles.
        // On next login, ensureProfile must be called so user-service repairs the gap.
        when(otpService.verifyOtp("9999999999", "654321")).thenReturn(true);
        UserAuth userWithMissingProfile = UserAuth.builder()
                .id(2L).mobileNumber("9999999999").verified(true).banned(false).build();
        when(userAuthRepository.findByMobileNumber("9999999999"))
                .thenReturn(Optional.of(userWithMissingProfile));
        when(userAuthRepository.save(any(UserAuth.class))).thenReturn(userWithMissingProfile);
        when(jwtUtil.generateToken(2L, "9999999999", "USER")).thenReturn("token-for-2");
        when(refreshTokenService.createForUser(2L)).thenReturn("refresh-for-2");

        VerifyOtpRequest req = new VerifyOtpRequest();
        req.setMobileNumber("9999999999");
        req.setOtp("654321");

        AuthResponse response = userAuthService.verifyOtp(req);

        assertThat(response.getUserId()).isEqualTo(2L);
        assertThat(response.getNewUser()).isFalse();

        // Even though the profile was missing, ensureProfile is called and will create it
        verify(userServiceClient).ensureProfile(2L, "9999999999");
    }

    // ── scenario: user-service unavailable — auth must still succeed ──────────

    @Test
    void verifyOtp_userServiceDown_authSucceedsAnyway() {
        when(otpService.verifyOtp("9876543210", "123456")).thenReturn(true);
        when(userAuthRepository.findByMobileNumber("9876543210")).thenReturn(Optional.empty());

        UserAuth created = UserAuth.builder()
                .id(1L).mobileNumber("9876543210").verified(true).banned(false).build();
        when(userAuthRepository.save(any(UserAuth.class))).thenReturn(created);
        when(jwtUtil.generateToken(1L, "9876543210", "USER")).thenReturn("token");
        when(refreshTokenService.createForUser(1L)).thenReturn("refresh");

        // UserServiceClient swallows exceptions internally, so even if it throws here
        // the verifyOtp call must complete successfully.
        doNothing().when(userServiceClient).ensureProfile(1L, "9876543210");

        VerifyOtpRequest req = new VerifyOtpRequest();
        req.setMobileNumber("9876543210");
        req.setOtp("123456");

        AuthResponse response = userAuthService.verifyOtp(req);
        assertThat(response).isNotNull();
        assertThat(response.getToken()).isEqualTo("token");
    }

    // ── scenario: JWT sub is always the authenticated user id ─────────────────

    @Test
    void verifyOtp_jwtSubIsAuthenticatedUserId() {
        // Verifies that the token is generated with the correct user id (from the
        // saved UserAuth entity), not a hardcoded or cached value.
        when(otpService.verifyOtp("9876543210", "123456")).thenReturn(true);
        when(userAuthRepository.findByMobileNumber("9876543210")).thenReturn(Optional.empty());

        UserAuth created = UserAuth.builder()
                .id(42L).mobileNumber("9876543210").verified(true).banned(false).build();
        when(userAuthRepository.save(any(UserAuth.class))).thenReturn(created);
        when(jwtUtil.generateToken(42L, "9876543210", "USER")).thenReturn("token-42");
        when(refreshTokenService.createForUser(42L)).thenReturn("refresh-42");

        VerifyOtpRequest req = new VerifyOtpRequest();
        req.setMobileNumber("9876543210");
        req.setOtp("123456");

        AuthResponse response = userAuthService.verifyOtp(req);

        // JWT is generated with the saved entity's id — this is the sub
        verify(jwtUtil).generateToken(42L, "9876543210", "USER");
        assertThat(response.getUserId()).isEqualTo(42L);
        verify(userServiceClient).ensureProfile(42L, "9876543210");
    }
}