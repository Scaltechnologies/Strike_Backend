package com.card_service.user.controller;

import com.card_service.common.dto.CardDefinitionResponse;
import com.card_service.common.response.ApiResponse;
import com.card_service.common.service.CardDefinitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cards")
@RequiredArgsConstructor
public class UserCardController {

    private final CardDefinitionService cardService;

    @GetMapping("/{id}")
    public ApiResponse<CardDefinitionResponse> getCard(@PathVariable Long id) {
        return ApiResponse.success(cardService.getCardById(id));
    }

    @GetMapping("/store/{storeId}")
    public ApiResponse<List<CardDefinitionResponse>> getActiveCardsByStore(@PathVariable Long storeId) {
        return ApiResponse.success(cardService.getActiveCardsByStore(storeId));
    }
}