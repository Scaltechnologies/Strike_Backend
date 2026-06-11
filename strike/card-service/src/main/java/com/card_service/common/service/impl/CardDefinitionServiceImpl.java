package com.card_service.common.service.impl;

import com.card_service.common.dto.CardDefinitionResponse;
import com.card_service.common.dto.CreateCardRequest;
import com.card_service.common.dto.UpdateCardRequest;
import com.card_service.common.entity.CardDefinition;
import com.card_service.common.exception.BadRequestException;
import com.card_service.common.exception.ResourceNotFoundException;
import com.card_service.common.repository.CardDefinitionRepository;
import com.card_service.common.service.CardDefinitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CardDefinitionServiceImpl implements CardDefinitionService {

    private final CardDefinitionRepository cardRepository;

    @Override
    public CardDefinitionResponse createCard(Long vendorId, CreateCardRequest request) {
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
        return toResponse(cardRepository.save(card));
    }

    @Override
    public CardDefinitionResponse getCardById(Long id) {
        return toResponse(findById(id));
    }

    @Override
    public List<CardDefinitionResponse> getCardsByStore(Long storeId) {
        return cardRepository.findByStoreId(storeId).stream().map(this::toResponse).toList();
    }

    @Override
    public List<CardDefinitionResponse> getActiveCardsByStore(Long storeId) {
        return cardRepository.findByStoreIdAndIsActiveTrue(storeId).stream().map(this::toResponse).toList();
    }

    @Override
    public List<CardDefinitionResponse> getCardsByVendor(Long vendorId) {
        return cardRepository.findByVendorId(vendorId).stream().map(this::toResponse).toList();
    }

    @Override
    public CardDefinitionResponse updateCard(Long id, Long vendorId, UpdateCardRequest request) {
        CardDefinition card = findById(id);
        if (!card.getVendorId().equals(vendorId)) {
            throw new BadRequestException("You do not own this card");
        }
        if (request.getName() != null) card.setName(request.getName());
        if (request.getDescription() != null) card.setDescription(request.getDescription());
        if (request.getCardPrice() != null) card.setCardPrice(request.getCardPrice());
        if (request.getWalletAmount() != null) card.setWalletAmount(request.getWalletAmount());
        if (request.getValidityInDays() != null) card.setValidityInDays(request.getValidityInDays());
        if (request.getImageUrl() != null) card.setImageUrl(request.getImageUrl());
        if (request.getIsActive() != null) card.setIsActive(request.getIsActive());
        return toResponse(cardRepository.save(card));
    }

    @Override
    public void deactivateCard(Long id, Long vendorId) {
        CardDefinition card = findById(id);
        if (!card.getVendorId().equals(vendorId)) {
            throw new BadRequestException("You do not own this card");
        }
        card.setIsActive(false);
        cardRepository.save(card);
    }

    private CardDefinition findById(Long id) {
        return cardRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Card not found with id: " + id));
    }

    private CardDefinitionResponse toResponse(CardDefinition card) {
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
                .createdAt(card.getCreatedAt())
                .build();
    }
}