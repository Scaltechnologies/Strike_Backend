package com.card_service.scheduler;

import com.card_service.common.service.SubscriptionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionExpiryJobTest {

    @Mock SubscriptionService subscriptionService;

    @InjectMocks SubscriptionExpiryJob subscriptionExpiryJob;

    @Test
    void expireSubscriptions_delegatesToService() {
        when(subscriptionService.expireOverdueSubscriptions()).thenReturn(5);

        subscriptionExpiryJob.expireSubscriptions();

        verify(subscriptionService).expireOverdueSubscriptions();
    }

    @Test
    void expireSubscriptions_zeroExpired_completesWithoutError() {
        when(subscriptionService.expireOverdueSubscriptions()).thenReturn(0);

        subscriptionExpiryJob.expireSubscriptions();

        verify(subscriptionService).expireOverdueSubscriptions();
    }

    @Test
    void expireSubscriptions_serviceThrows_exceptionSwallowed() {
        when(subscriptionService.expireOverdueSubscriptions())
                .thenThrow(new RuntimeException("DB connection lost"));

        // must not propagate — job should not crash the scheduler thread
        subscriptionExpiryJob.expireSubscriptions();

        verify(subscriptionService).expireOverdueSubscriptions();
    }
}