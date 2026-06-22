package com.redemption_service.common.service.impl;

import com.redemption_service.common.client.CardServiceClient;
import com.redemption_service.common.client.LedgerServiceClient;
import com.redemption_service.common.client.UserServiceClient;
import com.redemption_service.common.client.VendorServiceClient;
import com.redemption_service.common.client.VendorServiceClient.MenuItemInfo;
import com.redemption_service.common.dto.*;
import com.redemption_service.common.entity.RedemptionItem;
import com.redemption_service.common.entity.RedemptionRecord;
import com.redemption_service.common.enums.RedemptionStatus;
import com.redemption_service.common.exception.BadRequestException;
import com.redemption_service.common.exception.ResourceNotFoundException;
import com.redemption_service.common.repository.RedemptionRepository;
import com.redemption_service.common.response.PageResponse;
import com.redemption_service.common.service.RedemptionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class RedemptionServiceImpl implements RedemptionService {

    private final RedemptionRepository redemptionRepository;
    private final CardServiceClient cardServiceClient;
    private final VendorServiceClient vendorServiceClient;
    private final LedgerServiceClient ledgerServiceClient;
    private final UserServiceClient userServiceClient;
    private final RestTemplate restTemplate;

    @Value("${services.notification-url:http://localhost:8088}")
    private String notificationServiceUrl;

    // ── Legacy: vendor-POS direct redemption ─────────────────────────────────

    @Override
    @Transactional
    public RedemptionResponse redeem(Long vendorId, RedemptionRequest request) {
        if (!vendorServiceClient.verifyVendorOwnsStore(vendorId, request.getStoreId())) {
            throw new BadRequestException("You do not own store " + request.getStoreId());
        }

        SubscriptionRedemptionContext ctx = cardServiceClient.getRedemptionContext(request.getSubscriptionId());
        validateSubscriptionContext(ctx, request.getStoreId());

        RedemptionRecord record = RedemptionRecord.builder()
                .subscriptionId(request.getSubscriptionId())
                .userId(ctx.getUserId())
                .storeId(request.getStoreId())
                .totalAmount(BigDecimal.ZERO)
                .status(RedemptionStatus.COMPLETED)
                .initiatedBy("VENDOR")
                .build();

        BigDecimal total = validateAndPopulateItems(ctx, request.getItems(), request.getStoreId(), record);
        record.setTotalAmount(total);

        BigDecimal remainingBalance = cardServiceClient.deductBalance(request.getSubscriptionId(), total);
        RedemptionRecord saved = redemptionRepository.save(record);

        recordLedger(saved, total);
        sendNotification("/internal/notify/redemption", Map.of(
                "userId", ctx.getUserId(),
                "storeName", "Store #" + request.getStoreId(),
                "totalAmount", total,
                "remainingBalance", remainingBalance
        ));

        return toResponse(saved, remainingBalance);
    }

    // ── Phase 5: user submits request (no balance deduction) ─────────────────

    @Override
    @Transactional
    public RedemptionResponse requestRedemption(Long userId, RedemptionRequest request) {
        SubscriptionRedemptionContext ctx = cardServiceClient.getRedemptionContext(request.getSubscriptionId());

        if (!ctx.getUserId().equals(userId)) {
            throw new BadRequestException("This subscription does not belong to you.");
        }

        validateSubscriptionContext(ctx, request.getStoreId());

        if (redemptionRepository.existsBySubscriptionIdAndStatus(
                request.getSubscriptionId(), RedemptionStatus.PENDING)) {
            throw new BadRequestException(
                    "You already have a pending redemption request for this membership. " +
                    "Please wait for the store to process it.");
        }

        RedemptionRecord record = RedemptionRecord.builder()
                .subscriptionId(request.getSubscriptionId())
                .userId(userId)
                .storeId(request.getStoreId())
                .totalAmount(BigDecimal.ZERO)
                .status(RedemptionStatus.PENDING)
                .initiatedBy("USER")
                .build();

        BigDecimal total = validateAndPopulateItems(ctx, request.getItems(), request.getStoreId(), record);
        record.setTotalAmount(total);

        RedemptionRecord saved = redemptionRepository.save(record);
        return toResponse(saved, null);
    }

    // ── Phase 5: vendor approves (balance deducted here) ─────────────────────

    @Override
    @Transactional
    public RedemptionResponse approveRedemption(Long vendorId, Long redemptionId) {
        RedemptionRecord record = findById(redemptionId);

        if (!vendorServiceClient.verifyVendorOwnsStore(vendorId, record.getStoreId())) {
            throw new BadRequestException("You do not own the store for this redemption request.");
        }
        if (record.getStatus() != RedemptionStatus.PENDING) {
            throw new BadRequestException(
                    "Only pending requests can be approved. Current status: " + record.getStatus());
        }

        BigDecimal remainingBalance = cardServiceClient.deductBalance(
                record.getSubscriptionId(), record.getTotalAmount());

        record.setStatus(RedemptionStatus.COMPLETED);
        record.setApprovedAt(LocalDateTime.now());
        RedemptionRecord saved = redemptionRepository.save(record);

        recordLedger(saved, record.getTotalAmount());
        sendNotification("/internal/notify/redemption", Map.of(
                "userId", record.getUserId(),
                "storeName", "Store #" + record.getStoreId(),
                "totalAmount", record.getTotalAmount(),
                "remainingBalance", remainingBalance
        ));

        return toResponse(saved, remainingBalance);
    }

    // ── Phase 5: vendor rejects (no balance deduction) ────────────────────────

    @Override
    @Transactional
    public RedemptionResponse rejectRedemption(Long vendorId, Long redemptionId, String reason) {
        RedemptionRecord record = findById(redemptionId);

        if (!vendorServiceClient.verifyVendorOwnsStore(vendorId, record.getStoreId())) {
            throw new BadRequestException("You do not own the store for this redemption request.");
        }
        if (record.getStatus() != RedemptionStatus.PENDING) {
            throw new BadRequestException(
                    "Only pending requests can be rejected. Current status: " + record.getStatus());
        }

        record.setStatus(RedemptionStatus.REJECTED);
        record.setRejectedAt(LocalDateTime.now());
        record.setFailureReason(reason);
        RedemptionRecord saved = redemptionRepository.save(record);

        sendNotification("/internal/notify/redemption-rejected", Map.of(
                "userId", record.getUserId(),
                "storeName", "Store #" + record.getStoreId(),
                "reason", reason != null ? reason : ""
        ));

        return toResponse(saved, null);
    }

    // ── Phase 5: vendor views pending queue ───────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public List<RedemptionQueueResponse> getPendingQueue(Long storeId) {
        List<RedemptionRecord> records = redemptionRepository
                .findByStoreIdAndStatusOrderByCreatedAtAsc(storeId, RedemptionStatus.PENDING);
        Map<Long, String> customerNames = new HashMap<>();
        for (RedemptionRecord r : records) {
            customerNames.computeIfAbsent(r.getUserId(), userServiceClient::getCustomerName);
        }
        return records.stream()
                .map(r -> toQueueResponse(r, customerNames.get(r.getUserId())))
                .toList();
    }

    // ── Read ──────────────────────────────────────────────────────────────────

    @Override
    public RedemptionResponse getById(Long id) {
        return toResponse(findById(id), null);
    }

    @Override
    public PageResponse<RedemptionResponse> getBySubscription(Long subscriptionId, int page, int size) {
        return PageResponse.from(
                redemptionRepository.findBySubscriptionIdOrderByCreatedAtDesc(
                        subscriptionId, PageRequest.of(page, size))
                        .map(r -> toResponse(r, null)));
    }

    @Override
    public PageResponse<RedemptionResponse> getByStore(Long storeId, int page, int size) {
        return PageResponse.from(
                redemptionRepository.findByStoreIdOrderByCreatedAtDesc(storeId, PageRequest.of(page, size))
                        .map(r -> toResponse(r, null)));
    }

    @Override
    public PageResponse<RedemptionResponse> getByUser(Long userId, int page, int size) {
        return PageResponse.from(
                redemptionRepository.findByUserIdOrderByCreatedAtDesc(userId, PageRequest.of(page, size))
                        .map(r -> toResponse(r, null)));
    }

    @Override
    public PageResponse<RedemptionResponse> getAll(int page, int size) {
        return PageResponse.from(
                redemptionRepository.findAll(PageRequest.of(page, size))
                        .map(r -> toResponse(r, null)));
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /**
     * Validates subscription status and store binding. Shared between redeem() and requestRedemption().
     */
    private void validateSubscriptionContext(SubscriptionRedemptionContext ctx, Long requestedStoreId) {
        if (!"ACTIVE".equals(ctx.getStatus())) {
            throw new BadRequestException("Subscription is not active. Current status: " + ctx.getStatus());
        }
        if (!ctx.getStoreId().equals(requestedStoreId)) {
            throw new BadRequestException(
                    "This subscription is not valid at store " + requestedStoreId +
                    ". It was purchased for store " + ctx.getStoreId() + ".");
        }
    }

    /**
     * Validates each item against category and item-level eligibility, checks stock,
     * populates record.items in-place, and returns the computed total amount.
     */
    private BigDecimal validateAndPopulateItems(
            SubscriptionRedemptionContext ctx,
            List<RedemptionItemRequest> itemRequests,
            Long storeId,
            RedemptionRecord record) {

        List<Long> eligibleCategoryIds = ctx.getEligibleCategoryIds();
        if (eligibleCategoryIds == null || eligibleCategoryIds.isEmpty()) {
            throw new BadRequestException(
                    "This card has no menu categories configured. Please contact the vendor.");
        }
        Set<Long> eligibleCategories = new HashSet<>(eligibleCategoryIds);

        Set<Long> eligibleItemIds =
                (ctx.getEligibleMenuItemIds() != null && !ctx.getEligibleMenuItemIds().isEmpty())
                        ? new HashSet<>(ctx.getEligibleMenuItemIds()) : null;

        Map<Long, MenuItemInfo> menuItems = vendorServiceClient.getMenuItems(storeId);
        if (record.getItems() == null) {
            record.setItems(new ArrayList<>());
        }

        BigDecimal total = BigDecimal.ZERO;

        for (RedemptionItemRequest itemReq : itemRequests) {
            MenuItemInfo menuItem = menuItems.get(itemReq.getMenuItemId());
            if (menuItem == null) {
                throw new BadRequestException("Menu item not found: " + itemReq.getMenuItemId());
            }
            if (!menuItem.storeId().equals(storeId)) {
                throw new BadRequestException(
                        "Menu item does not belong to this store: " + menuItem.name());
            }
            if (menuItem.categoryId() == null || !eligibleCategories.contains(menuItem.categoryId())) {
                throw new BadRequestException(
                        "Item '" + menuItem.name() + "' is not eligible for redemption with this card. " +
                        "Only items from the card's mapped menu categories can be redeemed.");
            }
            if (eligibleItemIds != null && !eligibleItemIds.contains(menuItem.id())) {
                throw new BadRequestException(
                        "Item '" + menuItem.name() + "' is not eligible for redemption with this card. " +
                        "This card is restricted to specific menu items only.");
            }
            if ("OUT_OF_STOCK".equals(menuItem.availabilityStatus())) {
                throw new BadRequestException(
                        "Item '" + menuItem.name() + "' is currently out of stock and cannot be redeemed.");
            }

            BigDecimal itemTotal = menuItem.price().multiply(BigDecimal.valueOf(itemReq.getQuantity()));
            total = total.add(itemTotal);

            record.getItems().add(RedemptionItem.builder()
                    .redemptionRecord(record)
                    .menuItemId(menuItem.id())
                    .menuItemName(menuItem.name())
                    .quantity(itemReq.getQuantity())
                    .unitPrice(menuItem.price())
                    .totalPrice(itemTotal)
                    .build());
        }

        return total;
    }

    private void recordLedger(RedemptionRecord saved, BigDecimal total) {
        try {
            ledgerServiceClient.recordRedemption(
                    saved.getStoreId(),
                    saved.getUserId(),
                    saved.getSubscriptionId(),
                    total,
                    "Redemption #" + saved.getId()
            );
        } catch (Exception e) {
            log.error("Failed to record ledger entry for redemption #{} (subscriptionId={}, amount={}): {}",
                    saved.getId(), saved.getSubscriptionId(), total, e.getMessage(), e);
        }
    }

    private void sendNotification(String path, Map<String, Object> payload) {
        try {
            restTemplate.postForObject(notificationServiceUrl + path, payload, String.class);
        } catch (Exception e) {
            log.warn("Could not send notification [{}]: {}", path, e.getMessage());
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
                .initiatedBy(record.getInitiatedBy())
                .items(itemResponses)
                .createdAt(record.getCreatedAt())
                .approvedAt(record.getApprovedAt())
                .rejectedAt(record.getRejectedAt())
                .failureReason(record.getFailureReason())
                .build();
    }

    private RedemptionQueueResponse toQueueResponse(RedemptionRecord record, String customerName) {
        List<RedemptionItemResponse> itemResponses = record.getItems().stream()
                .map(i -> RedemptionItemResponse.builder()
                        .menuItemId(i.getMenuItemId())
                        .menuItemName(i.getMenuItemName())
                        .quantity(i.getQuantity())
                        .unitPrice(i.getUnitPrice())
                        .totalPrice(i.getTotalPrice())
                        .build())
                .toList();

        return RedemptionQueueResponse.builder()
                .id(record.getId())
                .subscriptionId(record.getSubscriptionId())
                .userId(record.getUserId())
                .customerName(customerName)
                .storeId(record.getStoreId())
                .totalAmount(record.getTotalAmount())
                .status(record.getStatus())
                .initiatedBy(record.getInitiatedBy())
                .items(itemResponses)
                .createdAt(record.getCreatedAt())
                .build();
    }
}
