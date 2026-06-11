package com.notification_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmsService {

    private final RestTemplate restTemplate;

    @Value("${notification.sms.enabled:false}")
    private boolean smsEnabled;

    @Value("${twilio.account-sid:}")
    private String accountSid;

    @Value("${twilio.auth-token:}")
    private String authToken;

    @Value("${twilio.from-number:}")
    private String fromNumber;

    /**
     * Sends an SMS. In dev mode (sms.enabled=false) just logs the message.
     * Returns "SENT", "MOCK", or "FAILED".
     */
    public String send(String toMobile, String message) {
        if (!smsEnabled) {
            log.info("[SMS MOCK] To={} | Message={}", toMobile, message);
            return "MOCK";
        }
        try {
            String url = "https://api.twilio.com/2010-04-01/Accounts/" + accountSid + "/Messages.json";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            String credentials = Base64.getEncoder().encodeToString((accountSid + ":" + authToken).getBytes());
            headers.set("Authorization", "Basic " + credentials);

            MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
            body.add("From", fromNumber);
            body.add("To", toMobile);
            body.add("Body", message);

            restTemplate.exchange(url, HttpMethod.POST, new HttpEntity<>(body, headers), String.class);
            log.info("[SMS SENT] To={}", toMobile);
            return "SENT";
        } catch (Exception e) {
            log.error("[SMS FAILED] To={} | Error={}", toMobile, e.getMessage());
            return "FAILED";
        }
    }
}