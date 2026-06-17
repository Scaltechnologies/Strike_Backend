package com.redemption_service.common.client;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Slf4j
@Component
@RequiredArgsConstructor
public class UserServiceClient {

    private final RestTemplate restTemplate;

    @Value("${services.user-url:http://localhost:8082}")
    private String userServiceUrl;

    /**
     * Fetches a display name for the vendor queue. Falls back gracefully so a
     * missing user-service endpoint never blocks approval queue rendering.
     */
    public String getCustomerName(Long userId) {
        try {
            String url = userServiceUrl + "/internal/users/" + userId + "/name";
            JsonNode response = restTemplate.getForObject(url, JsonNode.class);
            if (response != null && response.has("name") && !response.get("name").isNull()) {
                return response.get("name").asText();
            }
        } catch (Exception e) {
            log.warn("Could not fetch customer name for userId={}: {}", userId, e.getMessage());
        }
        return "Customer #" + userId;
    }
}
