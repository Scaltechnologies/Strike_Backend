package com.authservice.service;

import com.authservice.dto.RegisterVendorRequest;
import com.authservice.entity.Vendor;
import com.authservice.repository.VendorRepository;
import com.authservice.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class VendorAuthServiceTest {

    @Mock VendorRepository vendorRepository;
    @Mock OtpService otpService;
    @Mock JwtUtil jwtUtil;
    @Mock RefreshTokenService refreshTokenService;
    @Mock RestTemplate restTemplate;

    @InjectMocks VendorAuthService vendorAuthService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(vendorAuthService, "jwtExpirationMs", 3600000L);
        ReflectionTestUtils.setField(vendorAuthService, "vendorServiceUrl", "http://localhost:8083");
        ReflectionTestUtils.setField(vendorAuthService, "adminServiceUrl", "http://localhost:8087");
    }

    private RegisterVendorRequest buildRequest() {
        RegisterVendorRequest req = new RegisterVendorRequest();
        req.setHotelName("Test Hotel");
        req.setAddress("123 Main St");
        req.setMobileNumber("9876543210");
        req.setEmail("test@hotel.com");
        req.setLatitude(12.9716);
        req.setLongitude(77.5946);
        return req;
    }

    // ── registerVendor ───────────────────────────────────────────────────────

    @Test
    void registerVendor_alreadyActive_throwsRuntimeException() {
        Vendor active = Vendor.builder().mobileNumber("9876543210").status("ACTIVE").build();
        when(vendorRepository.findByMobileNumber("9876543210")).thenReturn(Optional.of(active));

        assertThatThrownBy(() -> vendorAuthService.registerVendor(buildRequest()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("already registered");
    }

    @Test
    void registerVendor_rejected_throwsRuntimeException() {
        Vendor rejected = Vendor.builder().mobileNumber("9876543210").status("REJECTED").build();
        when(vendorRepository.findByMobileNumber("9876543210")).thenReturn(Optional.of(rejected));

        assertThatThrownBy(() -> vendorAuthService.registerVendor(buildRequest()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("rejected");
    }

    @Test
    void registerVendor_suspended_throwsRuntimeException() {
        Vendor suspended = Vendor.builder().mobileNumber("9876543210").status("SUSPENDED").build();
        when(vendorRepository.findByMobileNumber("9876543210")).thenReturn(Optional.of(suspended));

        assertThatThrownBy(() -> vendorAuthService.registerVendor(buildRequest()))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("suspended");
    }

    @Test
    void registerVendor_newVendor_savesAndGeneratesOtp() {
        when(vendorRepository.findByMobileNumber("9876543210")).thenReturn(Optional.empty());

        vendorAuthService.registerVendor(buildRequest());

        verify(vendorRepository).save(any(Vendor.class));
        verify(otpService).generateOtp("9876543210", "VENDOR");
    }

    // ── sendLoginOtp ─────────────────────────────────────────────────────────

    @Test
    void sendLoginOtp_notRegistered_throwsRuntimeException() {
        when(vendorRepository.findByMobileNumber("9876543210")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vendorAuthService.sendLoginOtp("9876543210"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not registered");
    }

    @Test
    void sendLoginOtp_pendingStatus_throwsRuntimeException() {
        Vendor pending = Vendor.builder().mobileNumber("9876543210").status("PENDING").build();
        when(vendorRepository.findByMobileNumber("9876543210")).thenReturn(Optional.of(pending));

        assertThatThrownBy(() -> vendorAuthService.sendLoginOtp("9876543210"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not active");
    }

    @Test
    void sendLoginOtp_activeVendor_generatesOtp() {
        Vendor active = Vendor.builder().mobileNumber("9876543210").status("ACTIVE").build();
        when(vendorRepository.findByMobileNumber("9876543210")).thenReturn(Optional.of(active));

        vendorAuthService.sendLoginOtp("9876543210");

        verify(otpService).generateOtp("9876543210", "VENDOR");
    }

    // ── verifyOtp ────────────────────────────────────────────────────────────

    @Test
    void verifyOtp_invalidOtp_throwsRuntimeException() {
        when(otpService.verifyOtp("9876543210", "000000")).thenReturn(false);

        assertThatThrownBy(() -> vendorAuthService.verifyOtp("9876543210", "000000"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid");
    }

    @Test
    void verifyOtp_pendingVendor_promotesToVerifiedAndNotifiesAdmin() {
        when(otpService.verifyOtp("9876543210", "123456")).thenReturn(true);
        Vendor pending = Vendor.builder()
                .id(1L).mobileNumber("9876543210").hotelName("Hotel A")
                .email("a@hotel.com").status("PENDING").build();
        when(vendorRepository.findByMobileNumber("9876543210")).thenReturn(Optional.of(pending));

        var response = vendorAuthService.verifyOtp("9876543210", "123456");

        assertThat(response.getStatus()).isEqualTo("VERIFIED");
        assertThat(response.getMessage()).contains("Pending admin approval");
        verify(vendorRepository).save(pending);
        assertThat(pending.getStatus()).isEqualTo("VERIFIED");
    }

    @Test
    void verifyOtp_verifiedVendor_returnsWaitingMessage() {
        when(otpService.verifyOtp("9876543210", "123456")).thenReturn(true);
        Vendor verified = Vendor.builder()
                .id(2L).mobileNumber("9876543210").status("VERIFIED").build();
        when(vendorRepository.findByMobileNumber("9876543210")).thenReturn(Optional.of(verified));

        var response = vendorAuthService.verifyOtp("9876543210", "123456");

        assertThat(response.getStatus()).isEqualTo("VERIFIED");
        assertThat(response.getToken()).isNull();
    }

    @Test
    void verifyOtp_activeVendor_returnsAccessAndRefreshTokens() {
        when(otpService.verifyOtp("9876543210", "123456")).thenReturn(true);
        Vendor active = Vendor.builder()
                .id(3L).mobileNumber("9876543210").hotelName("Hotel B")
                .email("b@hotel.com").address("456 St").status("ACTIVE").build();
        when(vendorRepository.findByMobileNumber("9876543210")).thenReturn(Optional.of(active));
        when(jwtUtil.generateToken(3L, "9876543210", "VENDOR")).thenReturn("vendor.jwt");
        when(refreshTokenService.createForVendor(3L)).thenReturn("vendor-refresh-uuid");

        var response = vendorAuthService.verifyOtp("9876543210", "123456");

        assertThat(response.getToken()).isEqualTo("vendor.jwt");
        assertThat(response.getRefreshToken()).isEqualTo("vendor-refresh-uuid");
        assertThat(response.getExpiresIn()).isEqualTo(3600L);
    }

    @Test
    void verifyOtp_nonActiveNonPendingNonVerified_throwsRuntimeException() {
        when(otpService.verifyOtp("9876543210", "123456")).thenReturn(true);
        Vendor suspended = Vendor.builder()
                .id(4L).mobileNumber("9876543210").status("SUSPENDED").build();
        when(vendorRepository.findByMobileNumber("9876543210")).thenReturn(Optional.of(suspended));

        assertThatThrownBy(() -> vendorAuthService.verifyOtp("9876543210", "123456"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("SUSPENDED");
    }

    // ── approveVendor / rejectVendor / suspendVendor / reactivateVendor ──────

    @Test
    void approveVendor_setsStatusToActive() {
        Vendor vendor = Vendor.builder().id(1L).status("VERIFIED").build();
        when(vendorRepository.findById(1L)).thenReturn(Optional.of(vendor));

        vendorAuthService.approveVendor(1L);

        assertThat(vendor.getStatus()).isEqualTo("ACTIVE");
        verify(vendorRepository).save(vendor);
    }

    @Test
    void rejectVendor_notFound_throwsRuntimeException() {
        when(vendorRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> vendorAuthService.rejectVendor(99L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void reactivateVendor_notSuspended_throwsRuntimeException() {
        Vendor active = Vendor.builder().id(1L).status("ACTIVE").build();
        when(vendorRepository.findById(1L)).thenReturn(Optional.of(active));

        assertThatThrownBy(() -> vendorAuthService.reactivateVendor(1L))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("not suspended");
    }

    @Test
    void reactivateVendor_suspended_setsActiveAndSyncs() {
        Vendor suspended = Vendor.builder().id(1L).status("SUSPENDED").mobileNumber("9876543210").build();
        when(vendorRepository.findById(1L)).thenReturn(Optional.of(suspended));

        vendorAuthService.reactivateVendor(1L);

        assertThat(suspended.getStatus()).isEqualTo("ACTIVE");
        verify(vendorRepository).save(suspended);
    }
}