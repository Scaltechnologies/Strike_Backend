package com.card_service.common.service;

import com.card_service.common.dto.CardDefinitionResponse;
import com.card_service.common.dto.CreateCardRequest;
import com.card_service.common.dto.UpdateCardRequest;

import java.util.List;

public interface CardDefinitionService {
    CardDefinitionResponse createCard(Long vendorId, CreateCardRequest request);
    CardDefinitionResponse getCardById(Long id);
    List<CardDefinitionResponse> getCardsByStore(Long storeId);
    List<CardDefinitionResponse> getActiveCardsByStore(Long storeId);
    List<CardDefinitionResponse> getCardsByVendor(Long vendorId);
    CardDefinitionResponse updateCard(Long id, Long vendorId, UpdateCardRequest request);
    void deactivateCard(Long id, Long vendorId);
}