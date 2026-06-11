package com.card_service.service;

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
import com.card_service.common.service.impl.SubscriptionServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionServiceImplTest {

    @Mock ActiveSubscriptionRepository subscriptionRepository;
    @Mock CardDefinitionRepository cardRepository;
    @Mock LedgerServiceClient ledgerServiceClient;
    @Mock AdminServiceClient adminServiceClient;
    @Mock RestTemplate restTemplate;

    @InjectMocks SubscriptionServiceImpl subscriptionService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(subscriptionService, "notificationServiceUrl", "http://localhost:8088");
    }

    private CardDefinition activeCard() {
        return CardDefinition.builder()
                .id(1L).vendorId(10L).storeId(100L)
                .name("Lunch Card")
                .cardPrice(new BigDecimal("500.00"))
                .walletAmount(new BigDecimal("600.00"))
                .validityInDays(30)
                .isActive(true)
                .build();
    }

    private ActiveSubscription activeSubscription() {
        return ActiveSubscription.builder()
                .id(1L).userId(42L).cardDefinitionId(1L).storeId(100L)
                .walletBalance(new BigDecimal("600.00"))
                .status(SubscriptionStatus.ACTIVE)
                .purchasedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusDays(30))
                .build();
    }

    // ── purchase ─────────────────────────────────────────────────────────────

    @Test
    void purchase_cardNotFound_throwsResourceNotFoundException() {
        when(cardRepository.findById(1L)).thenReturn(Optional.empty());

        PurchaseSubscriptionRequest req = new PurchaseSubscriptionRequest();
        req.setCardDefinitionId(1L);
        req.setStoreId(100L);

        assertThatThrownBy(() -> subscriptionService.purchase(42L, req))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Card not found");
    }

    @Test
    void purchase_inactiveCard_throwsBadRequestException() {
        CardDefinition inactive = activeCard();
        inactive.setIsActive(false);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(inactive));

        PurchaseSubscriptionRequest req = new PurchaseSubscriptionRequest();
        req.setCardDefinitionId(1L);
        req.setStoreId(100L);

        assertThatThrownBy(() -> subscriptionService.purchase(42L, req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not currently available");
    }

    @Test
    void purchase_wrongStore_throwsBadRequestException() {
        when(cardRepository.findById(1L)).thenReturn(Optional.of(activeCard()));

        PurchaseSubscriptionRequest req = new PurchaseSubscriptionRequest();
        req.setCardDefinitionId(1L);
        req.setStoreId(999L); // wrong store

        assertThatThrownBy(() -> subscriptionService.purchase(42L, req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("does not belong");
    }

    @Test
    void purchase_success_savesSubscriptionAndRecordsLedgerAndCommission() {
        when(cardRepository.findById(1L)).thenReturn(Optional.of(activeCard()));
        ActiveSubscription saved = activeSubscription();
        when(subscriptionRepository.save(any(ActiveSubscription.class))).thenReturn(saved);

        PurchaseSubscriptionRequest req = new PurchaseSubscriptionRequest();
        req.setCardDefinitionId(1L);
        req.setStoreId(100L);

        SubscriptionResponse response = subscriptionService.purchase(42L, req);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        assertThat(response.getWalletBalance()).isEqualByComparingTo("600.00");
        verify(ledgerServiceClient).recordCardPurchase(eq(100L), eq(42L), eq(1L), any(), eq("Lunch Card"));
        verify(adminServiceClient).recordCommission(eq(10L), eq(100L), eq(1L), eq(42L), any());
    }

    // ── deductBalance ────────────────────────────────────────────────────────

    @Test
    void deductBalance_subscriptionNotActive_throwsBadRequestException() {
        ActiveSubscription cancelled = activeSubscription();
        cancelled.setStatus(SubscriptionStatus.CANCELLED);
        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(cancelled));

        assertThatThrownBy(() -> subscriptionService.deductBalance(1L, new BigDecimal("50")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("not active");
    }

    @Test
    void deductBalance_expired_marksExpiredAndThrowsBadRequest() {
        ActiveSubscription sub = activeSubscription();
        sub.setExpiresAt(LocalDateTime.now().minusDays(1));
        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(sub));
        when(subscriptionRepository.save(sub)).thenReturn(sub);

        assertThatThrownBy(() -> subscriptionService.deductBalance(1L, new BigDecimal("50")))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("expired");
        assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.EXPIRED);
    }

    @Test
    void deductBalance_insufficientBalance_throwsInsufficientBalanceException() {
        ActiveSubscription sub = activeSubscription(); // balance = 600
        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(sub));

        assertThatThrownBy(() -> subscriptionService.deductBalance(1L, new BigDecimal("700")))
                .isInstanceOf(InsufficientBalanceException.class)
                .hasMessageContaining("Insufficient balance");
    }

    @Test
    void deductBalance_success_subtractsAmountAndSaves() {
        ActiveSubscription sub = activeSubscription(); // balance = 600
        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(sub));
        when(subscriptionRepository.save(sub)).thenReturn(sub);

        BalanceResponse result = subscriptionService.deductBalance(1L, new BigDecimal("100"));

        assertThat(result.getBalance()).isEqualByComparingTo("500.00");
        assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.ACTIVE);
        verify(subscriptionRepository).save(sub);
    }

    @Test
    void deductBalance_balanceBecomesZero_marksExhausted() {
        ActiveSubscription sub = activeSubscription(); // balance = 600
        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(sub));
        when(subscriptionRepository.save(sub)).thenReturn(sub);

        subscriptionService.deductBalance(1L, new BigDecimal("600.00"));

        assertThat(sub.getStatus()).isEqualTo(SubscriptionStatus.EXHAUSTED);
    }

    // ── cancelSubscription ───────────────────────────────────────────────────

    @Test
    void cancelSubscription_wrongUser_throwsResourceNotFoundException() {
        ActiveSubscription sub = activeSubscription(); // userId = 42
        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(sub));

        assertThatThrownBy(() -> subscriptionService.cancelSubscription(1L, 99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void cancelSubscription_exhausted_throwsBadRequestException() {
        ActiveSubscription sub = activeSubscription();
        sub.setStatus(SubscriptionStatus.EXHAUSTED);
        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(sub));

        assertThatThrownBy(() -> subscriptionService.cancelSubscription(1L, 42L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("exhausted");
    }

    @Test
    void cancelSubscription_alreadyCancelled_throwsBadRequestException() {
        ActiveSubscription sub = activeSubscription();
        sub.setStatus(SubscriptionStatus.CANCELLED);
        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(sub));

        assertThatThrownBy(() -> subscriptionService.cancelSubscription(1L, 42L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("already cancelled");
    }

    @Test
    void cancelSubscription_success_marksCancelledAndSaves() {
        ActiveSubscription sub = activeSubscription();
        when(subscriptionRepository.findById(1L)).thenReturn(Optional.of(sub));
        when(subscriptionRepository.save(sub)).thenReturn(sub);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(activeCard()));

        SubscriptionResponse response = subscriptionService.cancelSubscription(1L, 42L);

        assertThat(response.getStatus()).isEqualTo(SubscriptionStatus.CANCELLED);
        verify(subscriptionRepository).save(sub);
    }

    // ── getActiveByUser (lazy expiry) ────────────────────────────────────────

    @Test
    void getActiveByUser_expiredSubscriptions_markedAndExcluded() {
        ActiveSubscription stillActive = activeSubscription();
        ActiveSubscription expired = activeSubscription();
        expired.setId(2L);
        expired.setExpiresAt(LocalDateTime.now().minusDays(1));

        when(subscriptionRepository.findByUserIdAndStatus(42L, SubscriptionStatus.ACTIVE))
                .thenReturn(List.of(stillActive, expired));
        when(subscriptionRepository.saveAll(any())).thenReturn(List.of(expired));
        when(cardRepository.findById(1L)).thenReturn(Optional.of(activeCard()));

        List<SubscriptionResponse> result = subscriptionService.getActiveByUser(42L);

        assertThat(result).hasSize(1);
        assertThat(expired.getStatus()).isEqualTo(SubscriptionStatus.EXPIRED);
        verify(subscriptionRepository).saveAll(argThat(list -> ((List<?>) list).size() == 1));
    }

    @Test
    void getActiveByUser_allActive_returnsAll() {
        ActiveSubscription s1 = activeSubscription();
        ActiveSubscription s2 = activeSubscription();
        s2.setId(2L);

        when(subscriptionRepository.findByUserIdAndStatus(42L, SubscriptionStatus.ACTIVE))
                .thenReturn(List.of(s1, s2));
        when(cardRepository.findById(1L)).thenReturn(Optional.of(activeCard()));

        List<SubscriptionResponse> result = subscriptionService.getActiveByUser(42L);

        assertThat(result).hasSize(2);
        verify(subscriptionRepository, never()).saveAll(any());
    }

    // ── expireOverdueSubscriptions ───────────────────────────────────────────

    @Test
    void expireOverdueSubscriptions_bulkExpiresAndReturnsCount() {
        ActiveSubscription expiring = activeSubscription();
        expiring.setExpiresAt(LocalDateTime.now().minusDays(1));

        when(subscriptionRepository.findByStatusAndExpiresAtBefore(
                eq(SubscriptionStatus.ACTIVE), any()))
                .thenReturn(List.of(expiring));
        when(subscriptionRepository.bulkExpire(any())).thenReturn(1);
        when(cardRepository.findById(1L)).thenReturn(Optional.of(activeCard()));

        int count = subscriptionService.expireOverdueSubscriptions();

        assertThat(count).isEqualTo(1);
        verify(subscriptionRepository).bulkExpire(any());
    }

    @Test
    void expireOverdueSubscriptions_noneExpiring_returnsZero() {
        when(subscriptionRepository.findByStatusAndExpiresAtBefore(
                eq(SubscriptionStatus.ACTIVE), any()))
                .thenReturn(List.of());
        when(subscriptionRepository.bulkExpire(any())).thenReturn(0);

        int count = subscriptionService.expireOverdueSubscriptions();

        assertThat(count).isEqualTo(0);
    }

    @Test
    void expireOverdueSubscriptions_notificationFails_bulkExpireStillRuns() {
        ActiveSubscription expiring = activeSubscription();
        expiring.setExpiresAt(LocalDateTime.now().minusDays(1));

        when(subscriptionRepository.findByStatusAndExpiresAtBefore(
                eq(SubscriptionStatus.ACTIVE), any()))
                .thenReturn(List.of(expiring));
        when(cardRepository.findById(1L)).thenReturn(Optional.of(activeCard()));
        when(restTemplate.postForObject(any(String.class), any(), eq(String.class)))
                .thenThrow(new RuntimeException("notification down"));
        when(subscriptionRepository.bulkExpire(any())).thenReturn(1);

        int count = subscriptionService.expireOverdueSubscriptions();

        assertThat(count).isEqualTo(1);
        verify(subscriptionRepository).bulkExpire(any());
    }
}