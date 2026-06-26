package com.user_service.profile.service;

import com.user_service.profile.dto.UserProfileRequest;
import com.user_service.profile.dto.UserProfileResponse;
import com.user_service.profile.entity.UserProfile;
import com.user_service.profile.repository.UserProfileRepository;
import com.user_service.profile.service.impl.UserProfileServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceImplTest {

    @Mock UserProfileRepository userProfileRepository;

    @InjectMocks UserProfileServiceImpl userProfileService;

    // ── getOrCreateProfile ────────────────────────────────────────────────────

    @Test
    void getOrCreateProfile_newUser_createsProfileWithMobile() {
        when(userProfileRepository.findById(1L)).thenReturn(Optional.empty());

        UserProfile saved = UserProfile.builder()
                .userId(1L).mobileNumber("9876543210").createdAt(LocalDateTime.now()).build();
        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(saved);

        UserProfileResponse response = userProfileService.getOrCreateProfile(1L, "9876543210");

        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getMobileNumber()).isEqualTo("9876543210");

        ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
        verify(userProfileRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(1L);
        assertThat(captor.getValue().getMobileNumber()).isEqualTo("9876543210");
    }

    @Test
    void getOrCreateProfile_existingProfile_returnsExistingWithoutSave() {
        UserProfile existing = UserProfile.builder()
                .userId(1L).mobileNumber("9876543210").name("Alice").createdAt(LocalDateTime.now()).build();
        when(userProfileRepository.findById(1L)).thenReturn(Optional.of(existing));

        UserProfileResponse response = userProfileService.getOrCreateProfile(1L, "9876543210");

        assertThat(response.getUserId()).isEqualTo(1L);
        assertThat(response.getName()).isEqualTo("Alice");

        // No save should happen when profile already exists
        verify(userProfileRepository, never()).save(any());
    }

    @Test
    void getOrCreateProfile_idempotent_secondCallReturnsExisting() {
        // First call: profile not found → created
        UserProfile created = UserProfile.builder()
                .userId(5L).mobileNumber("9999999999").createdAt(LocalDateTime.now()).build();
        when(userProfileRepository.findById(5L))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(created));
        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(created);

        userProfileService.getOrCreateProfile(5L, "9999999999");

        // Second call: profile now exists → no duplicate save
        userProfileService.getOrCreateProfile(5L, "9999999999");

        verify(userProfileRepository, times(1)).save(any(UserProfile.class));
    }

    // ── scenario: missing profile recovery ────────────────────────────────────

    @Test
    void getOrCreateProfile_missingProfile_isRepairedWithCorrectMobile() {
        // Simulates user_id=2 from the bug report: row in user_auth, no row in user_profiles
        when(userProfileRepository.findById(2L)).thenReturn(Optional.empty());

        UserProfile repaired = UserProfile.builder()
                .userId(2L).mobileNumber("9999999999").createdAt(LocalDateTime.now()).build();
        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(repaired);

        UserProfileResponse response = userProfileService.getOrCreateProfile(2L, "9999999999");

        assertThat(response.getUserId()).isEqualTo(2L);
        assertThat(response.getMobileNumber()).isEqualTo("9999999999");

        ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
        verify(userProfileRepository).save(captor.capture());
        assertThat(captor.getValue().getMobileNumber()).isEqualTo("9999999999");
    }

    // ── updateProfile ─────────────────────────────────────────────────────────

    @Test
    void updateProfile_existingProfile_updatesProvidedFields() {
        UserProfile existing = UserProfile.builder()
                .userId(1L).mobileNumber("9876543210").name("OldName").build();
        when(userProfileRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        UserProfileRequest req = new UserProfileRequest();
        req.setName("NewName");

        UserProfileResponse response = userProfileService.updateProfile(1L, req);

        assertThat(response.getName()).isEqualTo("NewName");
    }

    @Test
    void updateProfile_onlyUpdatesNonNullFields() {
        UserProfile existing = UserProfile.builder()
                .userId(1L).mobileNumber("9876543210").name("Alice").email("alice@example.com").build();
        when(userProfileRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        UserProfileRequest req = new UserProfileRequest();
        req.setName("AliceUpdated");
        // email not set → must not be overwritten

        UserProfileResponse response = userProfileService.updateProfile(1L, req);

        assertThat(response.getName()).isEqualTo("AliceUpdated");
        assertThat(response.getEmail()).isEqualTo("alice@example.com");
    }

    // ── updateLocation ─────────────────────────────────────────────────────────

    @Test
    void updateLocation_existingProfile_updatesCoordinates() {
        UserProfile existing = UserProfile.builder()
                .userId(1L).mobileNumber("9876543210").build();
        when(userProfileRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(userProfileRepository.save(any(UserProfile.class))).thenAnswer(inv -> inv.getArgument(0));

        UserProfileResponse response = userProfileService.updateLocation(1L, 12.9716, 77.5946);

        assertThat(response.getLatitude()).isEqualTo(12.9716);
        assertThat(response.getLongitude()).isEqualTo(77.5946);
        assertThat(response.getLastLocationAt()).isNotNull();
    }

    // ── scenario: purchase always uses authenticated user id ──────────────────

    @Test
    void getOrCreateProfile_userId_matchesJwtSub() {
        // The userId passed into getOrCreateProfile must equal the JWT sub value.
        // Gateway sets X-User-Id = JWT sub, controller reads X-User-Id.
        // This test verifies the profile is always keyed by the JWT sub.
        Long jwtSubUserId = 42L;

        when(userProfileRepository.findById(jwtSubUserId)).thenReturn(Optional.empty());
        UserProfile profile = UserProfile.builder()
                .userId(jwtSubUserId).mobileNumber("9876543210").createdAt(LocalDateTime.now()).build();
        when(userProfileRepository.save(any(UserProfile.class))).thenReturn(profile);

        UserProfileResponse response = userProfileService.getOrCreateProfile(jwtSubUserId, "9876543210");

        assertThat(response.getUserId()).isEqualTo(jwtSubUserId);

        ArgumentCaptor<UserProfile> captor = ArgumentCaptor.forClass(UserProfile.class);
        verify(userProfileRepository).save(captor.capture());
        assertThat(captor.getValue().getUserId()).isEqualTo(jwtSubUserId);
    }
}