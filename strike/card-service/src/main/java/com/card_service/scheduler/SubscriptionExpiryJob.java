package com.card_service.scheduler;

import com.card_service.common.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionExpiryJob {

    private final SubscriptionService subscriptionService;

    /**
     * Runs every hour at :00 by default.
     * Override with scheduler.expiry-cron in application.yaml.
     *
     * Spring cron: second minute hour day month weekday
     * "0 0 * * * *" = top of every hour
     */
    @Scheduled(cron = "${scheduler.expiry-cron:0 0 * * * *}")
    public void expireSubscriptions() {
        log.info("Subscription expiry job started at {}", LocalDateTime.now());
        try {
            int expired = subscriptionService.expireOverdueSubscriptions();
            if (expired > 0) {
                log.info("Subscription expiry job finished — {} subscription(s) expired", expired);
            } else {
                log.debug("Subscription expiry job finished — no subscriptions to expire");
            }
        } catch (Exception e) {
            log.error("Subscription expiry job failed: {}", e.getMessage(), e);
        }
    }
}