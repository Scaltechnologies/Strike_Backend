package com.redemption_service.common.service.impl;

import com.redemption_service.common.client.CardServiceClient;
import com.redemption_service.common.client.LedgerServiceClient;
import com.redemption_service.common.client.VendorServiceClient;
import com.redemption_service.common.client.VendorServiceClient.MenuItemInfo;
import com.redemption_service.common.dto.*;
import com.redemption_service.common.entity.RedemptionItem;
import com.redemption_service.common.entity.RedemptionRecord;
import com.redemption_service.common.enums.RedemptionStatus;
import com.redemption_service.common.exception.BadRequestException;
import com.redemption_service.common.exception.ResourceNotFoundException;
import com.redemption_service.common.repository.RedemptionRepository;
import com.redemption_service.common.service.RedemptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedemptionServiceImpl implements RedemptionService {

    private final RedemptionRepository redemptionRepository;
    private final CardServiceClient cardServiceClient;
    private final VendorServiceClient vendorServiceClient;
    private final LedgerServiceClient ledgerServiceClient;
    private final RestTemplate restTemplate;

    @Value("${services.notification-url:http://localhost:8088}")
    private String notificationServiceUrl;

    @Override
    @Transactional
    public RedemptionResponse redeem(RedemptionRequest request) {
        Map<Long, MenuItemInfo> menuItems = vendorServiceClient.getMenuItems(request.getStoreId());

        List<RedemptionItem> items = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        RedemptionRecord record = RedemptionRecord.builder()
                .subscriptionId(request.getSubscriptionId())
                .userId(request.getUserId())
                .storeId(request.getStoreId())
                .totalAmount(BigDecimal.ZERO)
                .status(RedemptionStatus.COMPLETED)
                .items(items)
                .build();

        for (RedemptionItemRequest itemReq : request.getItems()) {
            MenuItemInfo menuItem = menuItems.get(itemReq.getMenuItemId());
            if (menuItem == null) {
                throw new BadRequestException("Menu item not found: " + itemReq.getMenuItemId());
            }
            if (!menuItem.storeId().equals(request.getStoreId())) {
                throw new BadRequestException("Menu item does not belong to this store: " + menuItem.name());
            }

            BigDecimal itemTotal = menuItem.price().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            total = total.add(itemTotal);

            items.add(RedemptionItem.builder()
                    .redemptionRecord(record)
                    .menuItemId(menuItem.id())
                    .menuItemName(menuItem.name())
                    .quantity(itemReq.getQuantity())
                    .unitPrice(menuItem.price())
                    .totalPrice(itemTotal)
                    .build());
        }

        record.setTotalAmount(total);

        BigDecimal remainingBalance = cardServiceClient.deductBalance(request.getSubscriptionId(), total);

        RedemptionRecord saved = redemptionRepository.save(record);

        try {
            ledgerServiceClient.recordRedemption(
                    request.getStoreId(),
                    request.getUserId(),
                    request.getSubscriptionId(),
                    total,
                    "Redemption #" + saved.getId()
            );
        } catch (Exception ignored) {}

        sendRedemptionNotification(request.getUserId(), request.getStoreId(), total, remainingBalance);
        return toResponse(saved, remainingBalance);
    }

    @Override
    public RedemptionResponse getById(Long id) {
        RedemptionRecord record = findById(id);
        return toResponse(record, null);
    }

    @Override
    public List<RedemptionResponse> getBySubscription(Long subscriptionId) {
        return redemptionRepository.findBySubscriptionIdOrderByCreatedAtDesc(subscriptionId)
                .stream().map(r -> toResponse(r, null)).toList();
    }

    @Override
    public List<RedemptionResponse> getByStore(Long storeId) {
        return redemptionRepository.findByStoreIdOrderByCreatedAtDesc(storeId)
                .stream().map(r -> toResponse(r, null)).toList();
    }

    @Override
    public List<RedemptionResponse> getByUser(Long userId) {
        return redemptionRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream().map(r -> toResponse(r, null)).toList();
    }

    @Override
    public List<RedemptionResponse> getAll() {
        return redemptionRepository.findAll().stream()
                .map(r -> toResponse(r, null)).toList();
    }

    private void sendRedemptionNotification(Long userId, Long storeId, BigDecimal total, BigDecimal remaining) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("userId", userId);
            payload.put("storeName", "Store #" + storeId);
            payload.put("totalAmount", total);
            payload.put("remainingBalance", remaining);
            restTemplate.postForObject(
                    notificationServiceUrl + "/internal/notify/redemption", payload, String.class);
        } catch (Exception e) {
            log.warn("Could not send redemption notification for userId={}: {}", userId, e.getMessage());
        }
    }

    private RedemptionRecord findById(Long id) {
        return redemptionRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Redemption not found: " + id));
    }

    private RedemptionResponse toResponse(RedemptionRecord record, BigDecimal remainingBalance) {
        List<RedemptionItemResponse> itemResponses = record.getItems().stream()
                .map(i -> RedemptionItemResponse.builder()
                        .menuItemId(i.getMenuItemId())
                        .menuItemName(i.getMenuItemName())
                        .quantity(i.getQuantity())
                        .unitPrice(i.getUnitPrice())
                        .totalPrice(i.getTotalPrice())
                        .build())
                .toList();

        return RedemptionResponse.builder()
                .id(record.getId())
                .subscriptionId(record.getSubscriptionId())
                .userId(record.getUserId())
                .storeId(record.getStoreId())
                .totalAmount(record.getTotalAmount())
                .remainingBalance(remainingBalance)
                .status(record.getStatus())
                .items(itemResponses)
                .createdAt(record.getCreatedAt())
                .build();
    }
}