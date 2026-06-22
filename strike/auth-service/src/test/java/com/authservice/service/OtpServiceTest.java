package com.authservice.service;

import com.authservice.entity.OtpCode;
import com.authservice.repository.OtpRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OtpServiceTest {

    @Mock OtpRepository otpRepository;
    @Mock RestTemplate restTemplate;

    @InjectMocks OtpService otpService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(otpService, "notificationServiceUrl", "http://localhost:8088");
    }

    // ── generateOtp ──────────────────────────────────────────────────────────

    @Test
    void generateOtp_withinCooldown_throwsRuntimeException() {
        OtpCode recent = OtpCode.builder()
                .mobile("9876543210")
                .otpCode("111111")
                .createdAt(LocalDateTime.now().minusSeconds(30))
                .expiresAt(LocalDateTime.now().plusMinutes(5))
                .build();
        when(otpRepository.findTopByMobileOrderByCreatedAtDesc("9876543210"))
                .thenReturn(Optional.of(recent));

        assertThatThrownBy(() -> otpService.generateOtp("9876543210"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("wait");
        verify(otpRepository, never()).save(any());
    }

    @Test
    void generateOtp_hourlyCapReached_throwsRuntimeException() {
        when(otpRepository.findTopByMobileOrderByCreatedAtDesc("9876543210"))
                .thenReturn(Optional.empty());
        when(otpRepository.countByMobileAndCreatedAtAfter(eq("9876543210"), any()))
                .thenReturn(6);

        assertThatThrownBy(() -> otpService.generateOtp("9876543210"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Too many OTP");
        verify(otpRepository, never()).save(any());
    }

    @Test
    void generateOtp_success_savesOtpAndTriesNotification() {
        when(otpRepository.findTopByMobileOrderByCreatedAtDesc("9876543210"))
                .thenReturn(Optional.empty());
        when(otpRepository.countByMobileAndCreatedAtAfter(eq("9876543210"), any()))
                .thenReturn(2);

        ArgumentCaptor<OtpCode> captor = ArgumentCaptor.forClass(OtpCode.class);
        when(otpRepository.save(captor.capture())).thenAnswer(i -> i.getArgument(0));

        String otp = otpService.generateOtp("9876543210");

        assertThat(otp).hasSize(6).matches("\\d{6}");
        assertThat(captor.getValue().getMobile()).isEqualTo("9876543210");
        assertThat(captor.getValue().getOtpCode()).isEqualTo(otp);
        verify(restTemplate).postForObject(contains("/internal/notify/otp"), any(), eq(String.class));
    }

    @Test
    void generateOtp_notificationFails_otpStillSaved() {
        when(otpRepository.findTopByMobileOrderByCreatedAtDesc("9876543210"))
                .thenReturn(Optional.empty());
        when(otpRepository.countByMobileAndCreatedAtAfter(eq("9876543210"), any()))
                .thenReturn(0);
        when(otpRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(restTemplate.postForObject(any(String.class), any(), eq(String.class)))
                .thenThrow(new RuntimeException("connection refused"));

        String otp = otpService.generateOtp("9876543210");

        assertThat(otp).isNotBlank();
        verify(otpRepository).save(any());
    }

    @Test
    void generateOtp_afterCooldownExpired_succeeds() {
        OtpCode old = OtpCode.builder()
                .mobile("9876543210")
                .createdAt(LocalDateTime.now().minusSeconds(90))
                .build();
        when(otpRepository.findTopByMobileOrderByCreatedAtDesc("9876543210"))
                .thenReturn(Optional.of(old));
        when(otpRepository.countByMobileAndCreatedAtAfter(eq("9876543210"), any()))
                .thenReturn(1);
        when(otpRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        String otp = otpService.generateOtp("9876543210");
        assertThat(otp).hasSize(6);
    }

    // ── verifyOtp ────────────────────────────────────────────────────────────

    @Test
    void verifyOtp_noRecordFound_throwsNoSuchElementException() {
        when(otpRepository.findTopByMobileOrderByCreatedAtDesc("9876543210"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> otpService.verifyOtp("9876543210", "123456"))
                .isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void verifyOtp_expired_returnsFalse() {
        OtpCode expired = OtpCode.builder()
                .mobile("9876543210").otpCode("123456")
                .expiresAt(LocalDateTime.now().minusMinutes(1))
                .createdAt(LocalDateTime.now().minusMinutes(6))
                .verified(false).build();
        when(otpRepository.findTopByMobileOrderByCreatedAtDesc("9876543210"))
                .thenReturn(Optional.of(expired));

        assertThat(otpService.verifyOtp("9876543210", "123456")).isFalse();
    }

    @Test
    void verifyOtp_wrongCode_returnsFalse() {
        OtpCode code = OtpCode.builder()
                .mobile("9876543210").otpCode("123456")
                .expiresAt(LocalDateTime.now().plusMinutes(4))
                .createdAt(LocalDateTime.now().minusMinutes(1))
                .verified(false).build();
        when(otpRepository.findTopByMobileOrderByCreatedAtDesc("9876543210"))
                .thenReturn(Optional.of(code));

        assertThat(otpService.verifyOtp("9876543210", "999999")).isFalse();
    }

    @Test
    void verifyOtp_validCode_markesVerifiedAndReturnsTrue() {
        OtpCode code = OtpCode.builder()
                .mobile("9876543210").otpCode("123456")
                .expiresAt(LocalDateTime.now().plusMinutes(4))
                .createdAt(LocalDateTime.now().minusMinutes(1))
                .verified(false).build();
        when(otpRepository.findTopByMobileOrderByCreatedAtDesc("9876543210"))
                .thenReturn(Optional.of(code));

        boolean result = otpService.verifyOtp("9876543210", "123456");

        assertThat(result).isTrue();
        assertThat(code.getVerified()).isTrue();
        verify(otpRepository).save(code);
    }
}