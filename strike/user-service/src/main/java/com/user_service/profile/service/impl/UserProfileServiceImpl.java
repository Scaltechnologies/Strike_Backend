package com.user_service.profile.service.impl;

import com.user_service.profile.dto.UserProfileRequest;
import com.user_service.profile.dto.UserProfileResponse;
import com.user_service.profile.entity.UserProfile;
import com.user_service.profile.repository.UserProfileRepository;
import com.user_service.profile.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class UserProfileServiceImpl implements UserProfileService {

    private final UserProfileRepository userProfileRepository;

    @Override
    public UserProfileResponse getOrCreateProfile(Long userId, String mobile) {
        UserProfile profile = userProfileRepository.findById(userId)
                .orElseGet(() -> {
                    UserProfile newProfile = UserProfile.builder()
                            .userId(userId)
                            .mobileNumber(mobile)
                            .build();
                    return userProfileRepository.save(newProfile);
                });
        return toResponse(profile);
    }

    @Override
    public UserProfileResponse updateProfile(Long userId, UserProfileRequest request) {
        UserProfile profile = userProfileRepository.findById(userId)
                .orElseGet(() -> UserProfile.builder().userId(userId).build());
        if (request.getName() != null) profile.setName(request.getName());
        if (request.getEmail() != null) profile.setEmail(request.getEmail());
        if (request.getProfilePicUrl() != null) profile.setProfilePicUrl(request.getProfilePicUrl());
        return toResponse(userProfileRepository.save(profile));
    }

    @Override
    public UserProfileResponse updateLocation(Long userId, double latitude, double longitude) {
        UserProfile profile = userProfileRepository.findById(userId)
                .orElseGet(() -> UserProfile.builder().userId(userId).build());
        profile.setLatitude(latitude);
        profile.setLongitude(longitude);
        profile.setLastLocationAt(LocalDateTime.now());
        return toResponse(userProfileRepository.save(profile));
    }

    private UserProfileResponse toResponse(UserProfile profile) {
        return UserProfileResponse.builder()
                .userId(profile.getUserId())
                .name(profile.getName())
                .email(profile.getEmail())
                .mobileNumber(profile.getMobileNumber())
                .profilePicUrl(profile.getProfilePicUrl())
                .latitude(profile.getLatitude())
                .longitude(profile.getLongitude())
                .lastLocationAt(profile.getLastLocationAt())
                .createdAt(profile.getCreatedAt())
                .build();
    }
}