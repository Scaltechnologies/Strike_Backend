package com.authservice.client;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserServiceClient {

    private final RestTemplate restTemplate;

    @Value("${services.user-url}")
    private String userServiceUrl;

    /**
     * Creates a user_profiles row if one does not yet exist.
     * Idempotent — safe to call for both new and returning users.
     * On failure, logs a warning; auth success is never compromised.
     * The profile will be created lazily on the first GET /api/user/me call.
     */
    public void ensureProfile(Long userId, String mobile) {
        try {
            String url = userServiceUrl + "/internal/users/" + userId + "/ensure-profile";
            restTemplate.postForObject(url, Map.of("mobile", mobile), Object.class);
        } catch (Exception e) {
            log.warn("Could not ensure profile for userId={}: {}. Profile will be created on first GET /me.",
                    userId, e.getMessage());
        }
    }
}