package com.card_service.common.service.impl;

import com.card_service.common.client.AdminServiceClient;
import com.card_service.common.client.LedgerServiceClient;
import com.card_service.common.dto.BalanceResponse;
import com.card_service.common.dto.PurchaseSubscriptionRequest;
import com.card_service.common.dto.SubscriptionResponse;
import com.card_service.common.entity.ActiveSubscription;
import com.card_service.common.entity.CardDefinition;
import com.card_service.common.enums.SubscriptionStatus;
import com.card_service.common.exception.BadRequestException;
import com.card_service.common.exception.InsufficientBalanceException;
import com.card_service.common.exception.ResourceNotFoundException;
import com.card_service.common.repository.ActiveSubscriptionRepository;
import com.card_service.common.repository.CardDefinitionRepository;
import com.card_service.common.service.SubscriptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class SubscriptionServiceImpl implements SubscriptionService {

    private final ActiveSubscriptionRepository subscriptionRepository;
    private final CardDefinitionRepository cardRepository;
    private final LedgerServiceClient ledgerServiceClient;
    private final AdminServiceClient adminServiceClient;
    private final RestTemplate restTemplate;

    @Value("${services.notification-url:http://localhost:8088}")
    private String notificationServiceUrl;

    @Override
    @Transactional
    public SubscriptionResponse purchase(Long userId, PurchaseSubscriptionRequest request) {
        CardDefinition card = cardRepository.findById(request.getCardDefinitionId())
                .orElseThrow(() -> new ResourceNotFoundException("Card not found with id: " + request.getCardDefinitionId()));

        if (!card.getIsActive()) {
            throw new BadRequestException("This card is not currently available");
        }
        if (!card.getStoreId().equals(request.getStoreId())) {
            throw new BadRequestException("Card does not belong to the specified store");
        }

        LocalDateTime now = LocalDateTime.now();
        ActiveSubscription subscription = ActiveSubscription.builder()
                .userId(userId)
                .cardDefinitionId(card.getId())
                .storeId(card.getStoreId())
                .walletBalance(card.getWalletAmount())
                .status(SubscriptionStatus.ACTIVE)
                .purchasedAt(now)
                .expiresAt(now.plusDays(card.getValidityInDays()))
                .build();

        ActiveSubscription saved = subscriptionRepository.save(subscription);
        ledgerServiceClient.recordCardPurchase(
                card.getStoreId(), userId, saved.getId(), card.getCardPrice(), card.getName());
        adminServiceClient.recordCommission(
                card.getVendorId(), card.getStoreId(), saved.getId(), userId, card.getCardPrice());
        sendSubscriptionNotification(userId, card.getName(), card.getWalletAmount(), saved.getExpiresAt());
        return toResponse(saved, card.getName());
    }

    @Override
    public SubscriptionResponse getById(Long id) {
        ActiveSubscription sub = findById(id);
        return toResponse(sub, getCardName(sub.getCardDefinitionId()));
    }

    @Override
    public SubscriptionResponse getByIdForUser(Long id, Long userId) {
        ActiveSubscription sub = findById(id);
        if (!sub.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Subscription not found with id: " + id);
        }
        return toResponse(sub, getCardName(sub.getCardDefinitionId()));
    }

    @Override
    public List<SubscriptionResponse> getByUser(Long userId) {
        return subscriptionRepository.findByUserId(userId).stream()
                .map(sub -> toResponse(sub, getCardName(sub.getCardDefinitionId())))
                .toList();
    }

    @Override
    @Transactional
    public List<SubscriptionResponse> getActiveByUser(Long userId) {
        LocalDateTime now = LocalDateTime.now();
        List<ActiveSubscription> actives =
                subscriptionRepository.findByUserIdAndStatus(userId, SubscriptionStatus.ACTIVE);

        // lazily expire any that have passed their expiry date
        List<ActiveSubscription> expired = actives.stream()
                .filter(s -> s.getExpiresAt().isBefore(now))
                .peek(s -> s.setStatus(SubscriptionStatus.EXPIRED))
                .toList();
        if (!expired.isEmpty()) {
            subscriptionRepository.saveAll(expired);
        }

        return actives.stream()
                .filter(s -> s.getStatus() == SubscriptionStatus.ACTIVE)
                .map(sub -> toResponse(sub, getCardName(sub.getCardDefinitionId())))
                .toList();
    }

    @Override
    public List<SubscriptionResponse> getByStore(Long storeId) {
        return subscriptionRepository.findByStoreId(storeId).stream()
                .map(sub -> toResponse(sub, getCardName(sub.getCardDefinitionId())))
                .toList();
    }

    @Override
    public BalanceResponse getBalance(Long subscriptionId) {
        ActiveSubscription sub = findById(subscriptionId);
        return new BalanceResponse(subscriptionId, sub.getWalletBalance());
    }

    @Override
    @Transactional
    public BalanceResponse deductBalance(Long subscriptionId, BigDecimal amount) {
        ActiveSubscription sub = findById(subscriptionId);

        if (sub.getStatus() != SubscriptionStatus.ACTIVE) {
            throw new BadRequestException("Subscription is not active. Status: " + sub.getStatus());
        }
        if (sub.getExpiresAt().isBefore(LocalDateTime.now())) {
            sub.setStatus(SubscriptionStatus.EXPIRED);
            subscriptionRepository.save(sub);
            throw new BadRequestException("Subscription has expired");
        }
        if (sub.getWalletBalance().compareTo(amount) < 0) {
            throw new InsufficientBalanceException(
                    "Insufficient balance. Available: " + sub.getWalletBalance() + ", Required: " + amount);
        }

        BigDecimal newBalance = sub.getWalletBalance().subtract(amount);
        sub.setWalletBalance(newBalance);
        if (newBalance.compareTo(BigDecimal.ZERO) == 0) {
            sub.setStatus(SubscriptionStatus.EXHAUSTED);
        }

        subscriptionRepository.save(sub);
        return new BalanceResponse(subscriptionId, newBalance);
    }

    @Override
    @Transactional
    public SubscriptionResponse cancelSubscription(Long id, Long userId) {
        ActiveSubscription sub = findById(id);

        if (!sub.getUserId().equals(userId)) {
            throw new ResourceNotFoundException("Subscription not found with id: " + id);
        }
        if (sub.getStatus() == SubscriptionStatus.EXHAUSTED) {
            throw new BadRequestException("Cannot cancel an exhausted subscription");
        }
        if (sub.getStatus() == SubscriptionStatus.CANCELLED) {
            throw new BadRequestException("Subscription is already cancelled");
        }

        sub.setStatus(SubscriptionStatus.CANCELLED);
        return toResponse(subscriptionRepository.save(sub), getCardName(sub.getCardDefinitionId()));
    }

    private void sendSubscriptionNotification(Long userId, String cardName, BigDecimal balance, LocalDateTime expiresAt) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("userId", userId);
            payload.put("cardName", cardName);
            payload.put("storeName", "");
            payload.put("walletBalance", balance);
            payload.put("expiresAt", expiresAt != null ? expiresAt.toString() : null);
            restTemplate.postForObject(
                    notificationServiceUrl + "/internal/notify/subscription", payload, String.class);
        } catch (Exception e) {
            log.warn("Could not send subscription notification for userId={}: {}", userId, e.getMessage());
        }
    }

    private ActiveSubscription findById(Long id) {
        return subscriptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Subscription not found with id: " + id));
    }

    private String getCardName(Long cardId) {
        return cardRepository.findById(cardId).map(CardDefinition::getName).orElse("Unknown Card");
    }

    private SubscriptionResponse toResponse(ActiveSubscription sub, String cardName) {
        return SubscriptionResponse.builder()
                .id(sub.getId())
                .userId(sub.getUserId())
                .cardDefinitionId(sub.getCardDefinitionId())
                .cardName(cardName)
                .storeId(sub.getStoreId())
                .walletBalance(sub.getWalletBalance())
                .status(sub.getStatus())
                .purchasedAt(sub.getPurchasedAt())
                .expiresAt(sub.getExpiresAt())
                .createdAt(sub.getCreatedAt())
                .build();
    }
}