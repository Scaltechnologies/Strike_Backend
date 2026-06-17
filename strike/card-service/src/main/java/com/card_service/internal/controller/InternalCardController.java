package com.card_service.internal.controller;

import com.card_service.common.service.CardDefinitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/internal/cards")
@RequiredArgsConstructor
public class InternalCardController {

    private final CardDefinitionService cardService;

    @GetMapping("/{cardDefinitionId}/category-ids")
    public List<Long> getEligibleCategoryIds(@PathVariable Long cardDefinitionId) {
        return cardService.getEligibleCategoryIds(cardDefinitionId);
    }

    @GetMapping("/{cardDefinitionId}/menu-item-ids")
    public List<Long> getEligibleMenuItemIds(@PathVariable Long cardDefinitionId) {
        return cardService.getEligibleMenuItemIds(cardDefinitionId);
    }

    @GetMapping("/category-mappings/active/{categoryId}")
    public Map<String, Boolean> isCategoryMappedToActiveCard(@PathVariable Long categoryId) {
        return Map.of("mapped", cardService.isCategoryMappedToActiveCard(categoryId));
    }
}