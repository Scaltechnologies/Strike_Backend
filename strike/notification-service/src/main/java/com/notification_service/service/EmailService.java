package com.notification_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${notification.email.enabled:false}")
    private boolean emailEnabled;

    @Value("${notification.email.from:noreply@strikeapp.com}")
    private String fromEmail;

    /**
     * Sends an email. In dev mode (email.enabled=false) just logs the message.
     * Returns "SENT", "MOCK", or "FAILED".
     */
    public String send(String toEmail, String subject, String body) {
        if (!emailEnabled) {
            log.info("[EMAIL MOCK] To={} | Subject={} | Body={}", toEmail, subject, body);
            return "MOCK";
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(toEmail);
            message.setSubject(subject);
            message.setText(body);
            mailSender.send(message);
            log.info("[EMAIL SENT] To={} | Subject={}", toEmail, subject);
            return "SENT";
        } catch (Exception e) {
            log.error("[EMAIL FAILED] To={} | Error={}", toEmail, e.getMessage());
            return "FAILED";
        }
    }
}