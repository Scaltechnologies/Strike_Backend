package com.user_service.profile.service;

import com.user_service.profile.dto.UserProfileRequest;
import com.user_service.profile.dto.UserProfileResponse;

public interface UserProfileService {
    UserProfileResponse getOrCreateProfile(Long userId, String mobile);
    UserProfileResponse updateProfile(Long userId, UserProfileRequest request);
    UserProfileResponse updateLocation(Long userId, double latitude, double longitude);
}