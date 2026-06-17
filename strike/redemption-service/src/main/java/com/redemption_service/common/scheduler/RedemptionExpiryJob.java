package com.redemption_service.common.scheduler;

import com.redemption_service.common.entity.RedemptionRecord;
import com.redemption_service.common.enums.RedemptionStatus;
import com.redemption_service.common.repository.RedemptionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RedemptionExpiryJob {

    private final RedemptionRepository redemptionRepository;
    private final RestTemplate restTemplate;

    @Value("${services.notification-url:http://localhost:8088}")
    private String notificationServiceUrl;

    /**
     * Runs every 60 seconds. Finds PENDING requests older than 15 minutes and
     * moves them to REJECTED so the queue never fills with stale requests.
     */
    @Scheduled(fixedRate = 60_000)
    @Transactional
    public void expireStaleRequests() {
        LocalDateTime cutoff = LocalDateTime.now().minusMinutes(15);
        List<RedemptionRecord> stale =
                redemptionRepository.findByStatusAndCreatedAtBefore(RedemptionStatus.PENDING, cutoff);

        if (stale.isEmpty()) return;

        log.info("Expiring {} stale PENDING redemption request(s) older than 15 minutes", stale.size());

        for (RedemptionRecord record : stale) {
            record.setStatus(RedemptionStatus.REJECTED);
            record.setRejectedAt(LocalDateTime.now());
            record.setFailureReason("Request expired — no vendor action within 15 minutes. Please try again.");
            redemptionRepository.save(record);
            notifyExpiry(record.getUserId(), record.getStoreId());
        }
    }

    private void notifyExpiry(Long userId, Long storeId) {
        try {
            restTemplate.postForObject(
                    notificationServiceUrl + "/internal/notify/redemption-expired",
                    Map.of(
                            "userId", userId,
                            "storeName", "Store #" + storeId,
                            "message", "Your redemption request expired. Please visit the counter and try again."
                    ),
                    String.class
            );
        } catch (Exception e) {
            log.warn("Could not send expiry notification for userId={}: {}", userId, e.getMessage());
        }
    }
}
