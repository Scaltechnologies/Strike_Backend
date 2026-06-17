package com.card_service.common.service.impl;

import com.card_service.common.client.VendorServiceClient;
import com.card_service.common.dto.CardDefinitionResponse;
import com.card_service.common.dto.CardPreviewResponse;
import com.card_service.common.dto.CreateCardRequest;
import com.card_service.common.dto.EligibleItemInfo;
import com.card_service.common.dto.UpdateCardRequest;
import com.card_service.common.entity.CardCategoryMapping;
import com.card_service.common.entity.CardDefinition;
import com.card_service.common.entity.CardMenuItemMapping;
import com.card_service.common.exception.BadRequestException;
import com.card_service.common.exception.ResourceNotFoundException;
import com.card_service.common.repository.CardCategoryMappingRepository;
import com.card_service.common.repository.CardDefinitionRepository;
import com.card_service.common.repository.CardMenuItemMappingRepository;
import com.card_service.common.service.CardDefinitionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CardDefinitionServiceImpl implements CardDefinitionService {

    private final CardDefinitionRepository cardRepository;
    private final CardCategoryMappingRepository mappingRepository;
    private final CardMenuItemMappingRepository itemMappingRepository;
    private final VendorServiceClient vendorServiceClient;

    @Override
    @Transactional
    public CardDefinitionResponse createCard(Long vendorId, CreateCardRequest request) {
        List<Long> validCategoryIds = validateAndGetCategoryIds(request.getCategoryIds(), request.getStoreId());

        List<Long> validMenuItemIds = List.of();
        if (request.getEligibleMenuItemIds() != null && !request.getEligibleMenuItemIds().isEmpty()) {
            validMenuItemIds = validateAndGetMenuItemIds(
                    request.getEligibleMenuItemIds(), request.getStoreId(), validCategoryIds);
        }

        CardDefinition card = CardDefinition.builder()
                .vendorId(vendorId)
                .storeId(request.getStoreId())
                .name(request.getName())
                .description(request.getDescription())
                .cardPrice(request.getCardPrice())
                .walletAmount(request.getWalletAmount())
                .validityInDays(request.getValidityInDays())
                .imageUrl(request.getImageUrl())
                .isActive(true)
                .build();

        CardDefinition saved = cardRepository.save(card);
        saveCategoryMappings(saved.getId(), validCategoryIds);
        saveItemMappings(saved.getId(), validMenuItemIds);

        return buildResponse(saved, validCategoryIds, validMenuItemIds,
                resolveItemInfos(validMenuItemIds));
    }

    @Override
    public CardDefinitionResponse getCardById(Long id) {
        CardDefinition card = findById(id);
        List<Long> categoryIds = getCategoryIdsForCard(card.getId());
        List<Long> menuItemIds = getMenuItemIdsForCard(card.getId());
        return buildResponse(card, categoryIds, menuItemIds, resolveItemInfos(menuItemIds));
    }

    @Override
    public List<CardDefinitionResponse> getCardsByStore(Long storeId) {
        return toResponseList(cardRepository.findByStoreId(storeId));
    }

    @Override
    public List<CardDefinitionResponse> getActiveCardsByStore(Long storeId) {
        return toResponseList(cardRepository.findByStoreIdAndIsActiveTrue(storeId));
    }

    @Override
    public List<CardDefinitionResponse> getCardsByVendor(Long vendorId) {
        return toResponseList(cardRepository.findByVendorId(vendorId));
    }

    @Override
    @Transactional
    public CardDefinitionResponse updateCard(Long id, Long vendorId, UpdateCardRequest request) {
        CardDefinition card = findById(id);
        if (!card.getVendorId().equals(vendorId)) {
            throw new BadRequestException("You do not own this card");
        }

        if (request.getName() != null)          card.setName(request.getName());
        if (request.getDescription() != null)   card.setDescription(request.getDescription());
        if (request.getCardPrice() != null)      card.setCardPrice(request.getCardPrice());
        if (request.getWalletAmount() != null)   card.setWalletAmount(request.getWalletAmount());
        if (request.getValidityInDays() != null) card.setValidityInDays(request.getValidityInDays());
        if (request.getImageUrl() != null)       card.setImageUrl(request.getImageUrl());
        if (request.getIsActive() != null)       card.setIsActive(request.getIsActive());

        List<Long> categoryIds;
        if (request.getCategoryIds() != null) {
            List<Long> validIds = validateAndGetCategoryIds(request.getCategoryIds(), card.getStoreId());
            mappingRepository.deleteByCardDefinitionId(card.getId());
            saveCategoryMappings(card.getId(), validIds);
            categoryIds = validIds;
            // Category scope changed — existing item restrictions are no longer safe; clear them
            // unless the caller explicitly provides a new item list
            if (request.getEligibleMenuItemIds() == null) {
                itemMappingRepository.deleteByCardDefinitionId(card.getId());
            }
        } else {
            categoryIds = getCategoryIdsForCard(card.getId());
        }

        List<Long> menuItemIds;
        if (request.getEligibleMenuItemIds() != null) {
            List<Long> validItemIds = validateAndGetMenuItemIds(
                    request.getEligibleMenuItemIds(), card.getStoreId(), categoryIds);
            itemMappingRepository.deleteByCardDefinitionId(card.getId());
            saveItemMappings(card.getId(), validItemIds);
            menuItemIds = validItemIds;
        } else {
            menuItemIds = getMenuItemIdsForCard(card.getId());
        }

        return buildResponse(cardRepository.save(card), categoryIds, menuItemIds,
                resolveItemInfos(menuItemIds));
    }

    @Override
    @Transactional
    public void deactivateCard(Long id, Long vendorId) {
        CardDefinition card = findById(id);
        if (!card.getVendorId().equals(vendorId)) {
            throw new BadRequestException("You do not own this card");
        }
        card.setIsActive(false);
        cardRepository.save(card);
    }

    @Override
    public List<Long> getEligibleCategoryIds(Long cardDefinitionId) {
        return getCategoryIdsForCard(cardDefinitionId);
    }

    @Override
    public List<Long> getEligibleMenuItemIds(Long cardDefinitionId) {
        return getMenuItemIdsForCard(cardDefinitionId);
    }

    @Override
    public boolean isCategoryMappedToActiveCard(Long categoryId) {
        return mappingRepository.isMappedToActiveCard(categoryId);
    }

    @Override
    public CardPreviewResponse getCardPreview(Long cardDefinitionId) {
        CardDefinition card = findById(cardDefinitionId);
        List<Long> categoryIds = getCategoryIdsForCard(cardDefinitionId);
        List<Long> eligibleMenuItemIds = getMenuItemIdsForCard(cardDefinitionId);

        List<VendorServiceClient.CategoryInfo> categories;
        try {
            categories = vendorServiceClient.getCategoriesWithItems(categoryIds);
        } catch (Exception e) {
            throw new BadRequestException("Unable to fetch menu details for preview. Please try again.");
        }

        List<CardPreviewResponse.MenuCategoryPreview> menus = categories.stream()
                .map(cat -> CardPreviewResponse.MenuCategoryPreview.builder()
                        .categoryId(cat.id())
                        .categoryName(cat.name())
                        .items(cat.items().stream()
                                .filter(item -> eligibleMenuItemIds.isEmpty()
                                        || eligibleMenuItemIds.contains(item.id()))
                                .map(item -> CardPreviewResponse.MenuItemPreview.builder()
                                        .itemId(item.id())
                                        .name(item.name())
                                        .price(item.price())
                                        .itemType(item.itemType())
                                        .availabilityStatus(item.availabilityStatus())
                                        .build())
                                .toList())
                        .build())
                .filter(cat -> !cat.getItems().isEmpty())
                .toList();

        return CardPreviewResponse.builder()
                .cardId(card.getId())
                .cardName(card.getName())
                .description(card.getDescription())
                .cardPrice(card.getCardPrice())
                .walletAmount(card.getWalletAmount())
                .validityInDays(card.getValidityInDays())
                .eligibleMenus(menus)
                .build();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private List<Long> validateAndGetCategoryIds(List<Long> categoryIds, Long storeId) {
        List<Long> deduplicated = categoryIds.stream().distinct().toList();
        List<Long> validIds;
        try {
            validIds = vendorServiceClient.validateCategoryIds(deduplicated, storeId);
        } catch (Exception e) {
            log.error("Category validation failed — vendorServiceUrl={}, storeId={}, categoryIds={}: {} {}",
                    vendorServiceClient.getVendorServiceUrl(), storeId, deduplicated,
                    e.getClass().getSimpleName(), e.getMessage());
            throw new BadRequestException("Unable to validate menu categories. Please try again.");
        }
        if (validIds.size() != deduplicated.size()) {
            List<Long> invalid = new ArrayList<>(deduplicated);
            invalid.removeAll(validIds);
            throw new BadRequestException("Invalid or inactive menu category IDs: " + invalid);
        }
        return validIds;
    }

    private List<Long> validateAndGetMenuItemIds(List<Long> menuItemIds, Long storeId, List<Long> categoryIds) {
        List<Long> deduplicated = menuItemIds.stream().distinct().toList();
        List<Long> validIds;
        try {
            validIds = vendorServiceClient.validateMenuItemIds(deduplicated, storeId, categoryIds);
        } catch (Exception e) {
            log.error("Menu item validation failed — storeId={}, menuItemIds={}, categoryIds={}: {} {}",
                    storeId, deduplicated, categoryIds, e.getClass().getSimpleName(), e.getMessage());
            throw new BadRequestException("Unable to validate menu items. Please try again.");
        }
        if (validIds.size() != deduplicated.size()) {
            List<Long> invalid = new ArrayList<>(deduplicated);
            invalid.removeAll(validIds);
            throw new BadRequestException(
                    "Invalid menu item IDs (not found in this store or not in selected categories): " + invalid);
        }
        return validIds;
    }

    private void saveCategoryMappings(Long cardDefinitionId, List<Long> categoryIds) {
        List<CardCategoryMapping> mappings = categoryIds.stream()
                .map(catId -> CardCategoryMapping.builder()
                        .cardDefinitionId(cardDefinitionId)
                        .categoryId(catId)
                        .build())
                .toList();
        mappingRepository.saveAll(mappings);
    }

    private void saveItemMappings(Long cardDefinitionId, List<Long> menuItemIds) {
        if (menuItemIds.isEmpty()) return;
        List<CardMenuItemMapping> mappings = menuItemIds.stream()
                .map(itemId -> CardMenuItemMapping.builder()
                        .cardDefinitionId(cardDefinitionId)
                        .menuItemId(itemId)
                        .build())
                .toList();
        itemMappingRepository.saveAll(mappings);
    }

    private List<Long> getCategoryIdsForCard(Long cardDefinitionId) {
        return mappingRepository.findByCardDefinitionId(cardDefinitionId)
                .stream().map(CardCategoryMapping::getCategoryId).toList();
    }

    private List<Long> getMenuItemIdsForCard(Long cardDefinitionId) {
        return itemMappingRepository.findByCardDefinitionId(cardDefinitionId)
                .stream().map(CardMenuItemMapping::getMenuItemId).toList();
    }

    private List<CardDefinitionResponse> toResponseList(List<CardDefinition> cards) {
        if (cards.isEmpty()) return List.of();
        List<Long> cardIds = cards.stream().map(CardDefinition::getId).toList();
        Map<Long, List<Long>> categoryMap = buildCategoryMap(cardIds);
        Map<Long, List<Long>> itemMap = buildItemMap(cardIds);

        // Batch-resolve all item names in one vendor-service call
        List<Long> allItemIds = itemMap.values().stream().flatMap(List::stream).distinct().toList();
        Map<Long, EligibleItemInfo> itemInfoById;
        try {
            itemInfoById = vendorServiceClient.getMenuItemsByIds(allItemIds).stream()
                    .collect(Collectors.toMap(EligibleItemInfo::getId, i -> i));
        } catch (Exception e) {
            itemInfoById = Map.of();
        }
        final Map<Long, EligibleItemInfo> infoMap = itemInfoById;

        return cards.stream()
                .map(c -> {
                    List<Long> menuItemIds = itemMap.getOrDefault(c.getId(), List.of());
                    List<EligibleItemInfo> eligibleItems = menuItemIds.isEmpty() ? null :
                            menuItemIds.stream().map(infoMap::get)
                                    .filter(java.util.Objects::nonNull).toList();
                    return buildResponse(c,
                            categoryMap.getOrDefault(c.getId(), List.of()),
                            menuItemIds,
                            eligibleItems);
                })
                .toList();
    }

    private Map<Long, List<Long>> buildCategoryMap(List<Long> cardIds) {
        return mappingRepository.findByCardDefinitionIdIn(cardIds).stream()
                .collect(Collectors.groupingBy(
                        CardCategoryMapping::getCardDefinitionId,
                        Collectors.mapping(CardCategoryMapping::getCategoryId, Collectors.toList())
                ));
    }

    private Map<Long, List<Long>> buildItemMap(List<Long> cardIds) {
        return itemMappingRepository.findByCardDefinitionIdIn(cardIds).stream()
                .collect(Collectors.groupingBy(
                        CardMenuItemMapping::getCardDefinitionId,
                        Collectors.mapping(CardMenuItemMapping::getMenuItemId, Collectors.toList())
                ));
    }

    private CardDefinition findById(Long id) {
        return cardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found with id: " + id));
    }

    private List<EligibleItemInfo> resolveItemInfos(List<Long> menuItemIds) {
        if (menuItemIds.isEmpty()) return null;
        try {
            return vendorServiceClient.getMenuItemsByIds(menuItemIds);
        } catch (Exception e) {
            return null;
        }
    }

    private CardDefinitionResponse buildResponse(CardDefinition card, List<Long> categoryIds,
                                                  List<Long> menuItemIds, List<EligibleItemInfo> eligibleItems) {
        BigDecimal bonus = card.getWalletAmount().subtract(card.getCardPrice()).max(BigDecimal.ZERO);
        return CardDefinitionResponse.builder()
                .id(card.getId())
                .vendorId(card.getVendorId())
                .storeId(card.getStoreId())
                .name(card.getName())
                .description(card.getDescription())
                .cardPrice(card.getCardPrice())
                .walletAmount(card.getWalletAmount())
                .bonusAmount(bonus)
                .validityInDays(card.getValidityInDays())
                .imageUrl(card.getImageUrl())
                .isActive(card.getIsActive())
                .categoryIds(categoryIds)
                .eligibleMenuItemIds(menuItemIds.isEmpty() ? null : menuItemIds)
                .eligibleItems(eligibleItems)
                .createdAt(card.getCreatedAt())
                .build();
    }
}