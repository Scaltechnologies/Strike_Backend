package com.authservice.service;

import com.authservice.entity.RefreshToken;
import com.authservice.repository.RefreshTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock RefreshTokenRepository refreshTokenRepository;

    @InjectMocks RefreshTokenService refreshTokenService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(refreshTokenService, "refreshExpirationMs", 2592000000L);
    }

    // ── createForUser ────────────────────────────────────────────────────────

    @Test
    void createForUser_deletesExistingAndSavesNew() {
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        when(refreshTokenRepository.save(captor.capture()))
                .thenAnswer(i -> i.getArgument(0));

        String token = refreshTokenService.createForUser(42L);

        verify(refreshTokenRepository).deleteByUserId(42L);
        assertThat(token).isNotBlank();
        RefreshToken saved = captor.getValue();
        assertThat(saved.getUserId()).isEqualTo(42L);
        assertThat(saved.getRole()).isEqualTo("USER");
        assertThat(saved.getExpiresAt()).isAfter(LocalDateTime.now().plusDays(29));
    }

    // ── createForVendor ──────────────────────────────────────────────────────

    @Test
    void createForVendor_deletesExistingAndSavesNew() {
        ArgumentCaptor<RefreshToken> captor = ArgumentCaptor.forClass(RefreshToken.class);
        when(refreshTokenRepository.save(captor.capture()))
                .thenAnswer(i -> i.getArgument(0));

        String token = refreshTokenService.createForVendor(99L);

        verify(refreshTokenRepository).deleteByVendorId(99L);
        assertThat(token).isNotBlank();
        assertThat(captor.getValue().getRole()).isEqualTo("VENDOR");
        assertThat(captor.getValue().getVendorId()).isEqualTo(99L);
    }

    // ── validate ─────────────────────────────────────────────────────────────

    @Test
    void validate_tokenNotFound_throwsRuntimeException() {
        when(refreshTokenRepository.findByToken("bad-token")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> refreshTokenService.validate("bad-token"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Invalid refresh token");
    }

    @Test
    void validate_tokenExpired_deletesAndThrows() {
        RefreshToken expired = RefreshToken.builder()
                .token("expired-token").userId(1L).role("USER")
                .expiresAt(LocalDateTime.now().minusDays(1))
                .createdAt(LocalDateTime.now().minusDays(31))
                .build();
        when(refreshTokenRepository.findByToken("expired-token"))
                .thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> refreshTokenService.validate("expired-token"))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("expired");
        verify(refreshTokenRepository).deleteByToken("expired-token");
    }

    @Test
    void validate_validToken_returnsToken() {
        RefreshToken valid = RefreshToken.builder()
                .token("valid-token").userId(1L).role("USER")
                .expiresAt(LocalDateTime.now().plusDays(15))
                .createdAt(LocalDateTime.now().minusDays(15))
                .build();
        when(refreshTokenRepository.findByToken("valid-token"))
                .thenReturn(Optional.of(valid));

        RefreshToken result = refreshTokenService.validate("valid-token");

        assertThat(result.getToken()).isEqualTo("valid-token");
        verify(refreshTokenRepository, never()).deleteByToken(any());
    }

    // ── revoke ───────────────────────────────────────────────────────────────

    @Test
    void revoke_callsDeleteByToken() {
        refreshTokenService.revoke("some-token");
        verify(refreshTokenRepository).deleteByToken("some-token");
    }
}