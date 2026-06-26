package com.user_service.profile.controller;

import com.user_service.profile.dto.EnsureProfileRequest;
import com.user_service.profile.dto.UserProfileResponse;
import com.user_service.profile.entity.UserProfile;
import com.user_service.profile.repository.UserProfileRepository;
import com.user_service.profile.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalUserProfileController {

    private final UserProfileRepository userProfileRepository;
    private final UserProfileService userProfileService;

    @GetMapping
    public List<UserProfileResponse> getAllProfiles() {
        return userProfileRepository.findAll().stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/{userId}/profile")
    public ResponseEntity<UserProfileResponse> getProfile(@PathVariable Long userId) {
        return userProfileRepository.findById(userId)
                .map(p -> ResponseEntity.ok(toResponse(p)))
                .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Creates a user_profiles row for the given userId if one does not exist.
     * Idempotent — returns the existing profile if already present.
     * Called by auth-service during OTP verification to guarantee consistency.
     */
    @PostMapping("/{userId}/ensure-profile")
    public UserProfileResponse ensureProfile(
            @PathVariable Long userId,
            @RequestBody EnsureProfileRequest request) {
        return userProfileService.getOrCreateProfile(userId, request.getMobile());
    }

    private UserProfileResponse toResponse(UserProfile p) {
        return UserProfileResponse.builder()
                .userId(p.getUserId())
                .name(p.getName())
                .email(p.getEmail())
                .mobileNumber(p.getMobileNumber())
                .profilePicUrl(p.getProfilePicUrl())
                .latitude(p.getLatitude())
                .longitude(p.getLongitude())
                .lastLocationAt(p.getLastLocationAt())
                .createdAt(p.getCreatedAt())
                .build();
    }
}