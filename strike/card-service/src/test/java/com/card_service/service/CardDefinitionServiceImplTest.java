package com.card_service.service;

import com.card_service.common.client.VendorServiceClient;
import com.card_service.common.dto.*;
import com.card_service.common.entity.CardCategoryMapping;
import com.card_service.common.entity.CardDefinition;
import com.card_service.common.exception.BadRequestException;
import com.card_service.common.exception.ResourceNotFoundException;
import com.card_service.common.repository.CardCategoryMappingRepository;
import com.card_service.common.repository.CardDefinitionRepository;
import com.card_service.common.service.impl.CardDefinitionServiceImpl;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CardDefinitionServiceImplTest {

    @Mock CardDefinitionRepository cardRepository;
    @Mock CardCategoryMappingRepository mappingRepository;
    @Mock VendorServiceClient vendorServiceClient;

    @InjectMocks CardDefinitionServiceImpl cardService;

    // ── helpers ──────────────────────────────────────────────────────────────

    private CardDefinition card() {
        return CardDefinition.builder()
                .id(1L).vendorId(10L).storeId(100L)
                .name("Lunch Card").description("desc")
                .cardPrice(new BigDecimal("500.00"))
                .walletAmount(new BigDecimal("600.00"))
                .validityInDays(30)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private CreateCardRequest createRequest() {
        CreateCardRequest req = new CreateCardRequest();
        req.setStoreId(100L);
        req.setName("Lunch Card");
        req.setCardPrice(new BigDecimal("500.00"));
        req.setWalletAmount(new BigDecimal("600.00"));
        req.setValidityInDays(30);
        req.setCategoryIds(List.of(1L, 2L));
        return req;
    }

    private CardCategoryMapping mapping(Long categoryId) {
        return CardCategoryMapping.builder()
                .id(categoryId).cardDefinitionId(1L).categoryId(categoryId).build();
    }

    // ── createCard ───────────────────────────────────────────────────────────

    @Test
    void createCard_success_savesCardAndMappings() {
        when(vendorServiceClient.validateCategoryIds(List.of(1L, 2L), 100L))
                .thenReturn(List.of(1L, 2L));
        CardDefinition saved = card();
        when(cardRepository.save(any(CardDefinition.class))).thenReturn(saved);
        when(mappingRepository.saveAll(anyList())).thenReturn(List.of());

        CardDefinitionResponse response = cardService.createCard(10L, createRequest());

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getCategoryIds()).containsExactlyInAnyOrder(1L, 2L);
        verify(cardRepository).save(any(CardDefinition.class));
        verify(mappingRepository).saveAll(argThat(list -> ((List<?>) list).size() == 2));
    }

    @Test
    void createCard_duplicateCategoryIds_deduplicatedBeforeValidation() {
        when(vendorServiceClient.validateCategoryIds(List.of(1L, 2L), 100L))
                .thenReturn(List.of(1L, 2L));
        when(cardRepository.save(any())).thenReturn(card());
        when(mappingRepository.saveAll(anyList())).thenReturn(List.of());

        CreateCardRequest req = createRequest();
        req.setCategoryIds(List.of(1L, 2L, 1L)); // duplicate 1L

        cardService.createCard(10L, req);

        verify(vendorServiceClient).validateCategoryIds(List.of(1L, 2L), 100L);
    }

    @Test
    void createCard_someInvalidCategoryIds_throwsBadRequestException() {
        when(vendorServiceClient.validateCategoryIds(List.of(1L, 2L), 100L))
                .thenReturn(List.of(1L)); // 2L invalid

        assertThatThrownBy(() -> cardService.createCard(10L, createRequest()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid or inactive menu category IDs")
                .hasMessageContaining("2");
    }

    @Test
    void createCard_vendorServiceThrows_wrappedAsBadRequestException() {
        when(vendorServiceClient.validateCategoryIds(anyList(), anyLong()))
                .thenThrow(new RuntimeException("vendor-service down"));

        assertThatThrownBy(() -> cardService.createCard(10L, createRequest()))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Unable to validate menu categories");
    }

    // ── getCardById ──────────────────────────────────────────────────────────

    @Test
    void getCardById_found_returnsMappedResponse() {
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card()));
        when(mappingRepository.findByCardDefinitionId(1L))
                .thenReturn(List.of(mapping(1L), mapping(2L)));

        CardDefinitionResponse response = cardService.getCardById(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getCategoryIds()).containsExactlyInAnyOrder(1L, 2L);
        assertThat(response.getBonusAmount()).isEqualByComparingTo("100.00");
    }

    @Test
    void getCardById_notFound_throwsResourceNotFoundException() {
        when(cardRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.getCardById(99L))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("99");
    }

    // ── getCardsByStore / getActiveCardsByStore / getCardsByVendor ────────────

    @Test
    void getCardsByStore_returnsMappedResponses() {
        when(cardRepository.findByStoreId(100L)).thenReturn(List.of(card()));
        when(mappingRepository.findByCardDefinitionIdIn(List.of(1L)))
                .thenReturn(List.of(mapping(1L)));

        List<CardDefinitionResponse> results = cardService.getCardsByStore(100L);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getCategoryIds()).containsExactly(1L);
    }

    @Test
    void getCardsByStore_empty_returnsEmptyList() {
        when(cardRepository.findByStoreId(100L)).thenReturn(List.of());

        assertThat(cardService.getCardsByStore(100L)).isEmpty();
        verifyNoInteractions(mappingRepository);
    }

    @Test
    void getActiveCardsByStore_returnsOnlyActiveCards() {
        when(cardRepository.findByStoreIdAndIsActiveTrue(100L)).thenReturn(List.of(card()));
        when(mappingRepository.findByCardDefinitionIdIn(List.of(1L))).thenReturn(List.of());

        List<CardDefinitionResponse> results = cardService.getActiveCardsByStore(100L);

        assertThat(results).hasSize(1);
    }

    // ── updateCard ───────────────────────────────────────────────────────────

    @Test
    void updateCard_nameOnly_updatesNameAndKeepsExistingCategories() {
        CardDefinition existing = card();
        when(cardRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(cardRepository.save(existing)).thenReturn(existing);
        when(mappingRepository.findByCardDefinitionId(1L))
                .thenReturn(List.of(mapping(1L), mapping(2L)));

        UpdateCardRequest req = new UpdateCardRequest();
        req.setName("Dinner Card");

        CardDefinitionResponse response = cardService.updateCard(1L, 10L, req);

        assertThat(response.getName()).isEqualTo("Dinner Card");
        assertThat(response.getCategoryIds()).containsExactlyInAnyOrder(1L, 2L);
        verifyNoInteractions(vendorServiceClient);
        verify(mappingRepository, never()).deleteByCardDefinitionId(anyLong());
    }

    @Test
    void updateCard_withNewCategories_replacesOldMappings() {
        CardDefinition existing = card();
        when(cardRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(cardRepository.save(existing)).thenReturn(existing);
        when(vendorServiceClient.validateCategoryIds(List.of(3L, 4L), 100L))
                .thenReturn(List.of(3L, 4L));
        when(mappingRepository.saveAll(anyList())).thenReturn(List.of());

        UpdateCardRequest req = new UpdateCardRequest();
        req.setCategoryIds(List.of(3L, 4L));

        CardDefinitionResponse response = cardService.updateCard(1L, 10L, req);

        verify(mappingRepository).deleteByCardDefinitionId(1L);
        verify(mappingRepository).saveAll(argThat(list -> ((List<?>) list).size() == 2));
        assertThat(response.getCategoryIds()).containsExactlyInAnyOrder(3L, 4L);
    }

    @Test
    void updateCard_wrongVendor_throwsBadRequestException() {
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card())); // vendorId=10

        UpdateCardRequest req = new UpdateCardRequest();
        req.setName("New Name");

        assertThatThrownBy(() -> cardService.updateCard(1L, 99L, req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("do not own");
    }

    @Test
    void updateCard_invalidNewCategories_throwsBadRequestException() {
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card()));
        when(vendorServiceClient.validateCategoryIds(List.of(5L), 100L))
                .thenReturn(List.of()); // 5L invalid

        UpdateCardRequest req = new UpdateCardRequest();
        req.setCategoryIds(List.of(5L));

        assertThatThrownBy(() -> cardService.updateCard(1L, 10L, req))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Invalid or inactive");
    }

    // ── deactivateCard ────────────────────────────────────────────────────────

    @Test
    void deactivateCard_success_setsIsActiveFalseAndSaves() {
        CardDefinition existing = card();
        when(cardRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(cardRepository.save(existing)).thenReturn(existing);

        cardService.deactivateCard(1L, 10L);

        assertThat(existing.getIsActive()).isFalse();
        verify(cardRepository).save(existing);
    }

    @Test
    void deactivateCard_wrongVendor_throwsBadRequestException() {
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card())); // vendorId=10

        assertThatThrownBy(() -> cardService.deactivateCard(1L, 99L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("do not own");
    }

    // ── getEligibleCategoryIds ────────────────────────────────────────────────

    @Test
    void getEligibleCategoryIds_returnsIdsFromMappings() {
        when(mappingRepository.findByCardDefinitionId(1L))
                .thenReturn(List.of(mapping(1L), mapping(2L), mapping(3L)));

        List<Long> ids = cardService.getEligibleCategoryIds(1L);

        assertThat(ids).containsExactlyInAnyOrder(1L, 2L, 3L);
    }

    @Test
    void getEligibleCategoryIds_noMappings_returnsEmptyList() {
        when(mappingRepository.findByCardDefinitionId(1L)).thenReturn(List.of());

        assertThat(cardService.getEligibleCategoryIds(1L)).isEmpty();
    }

    // ── isCategoryMappedToActiveCard ─────────────────────────────────────────

    @Test
    void isCategoryMappedToActiveCard_mapped_returnsTrue() {
        when(mappingRepository.isMappedToActiveCard(5L)).thenReturn(true);

        assertThat(cardService.isCategoryMappedToActiveCard(5L)).isTrue();
    }

    @Test
    void isCategoryMappedToActiveCard_notMapped_returnsFalse() {
        when(mappingRepository.isMappedToActiveCard(5L)).thenReturn(false);

        assertThat(cardService.isCategoryMappedToActiveCard(5L)).isFalse();
    }

    // ── getCardPreview ────────────────────────────────────────────────────────

    @Test
    void getCardPreview_success_returnsPreviewWithMenus() {
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card()));
        when(mappingRepository.findByCardDefinitionId(1L))
                .thenReturn(List.of(mapping(1L), mapping(2L)));

        VendorServiceClient.ItemInfo item = new VendorServiceClient.ItemInfo(
                10L, "Burger", new BigDecimal("120.00"), "VEG", "AVAILABLE");
        VendorServiceClient.CategoryInfo cat = new VendorServiceClient.CategoryInfo(
                1L, "Fast Food", List.of(item));
        when(vendorServiceClient.getCategoriesWithItems(List.of(1L, 2L)))
                .thenReturn(List.of(cat));

        CardPreviewResponse preview = cardService.getCardPreview(1L);

        assertThat(preview.getCardId()).isEqualTo(1L);
        assertThat(preview.getCardName()).isEqualTo("Lunch Card");
        assertThat(preview.getEligibleMenus()).hasSize(1);
        assertThat(preview.getEligibleMenus().get(0).getCategoryName()).isEqualTo("Fast Food");
        assertThat(preview.getEligibleMenus().get(0).getItems()).hasSize(1);
        assertThat(preview.getEligibleMenus().get(0).getItems().get(0).getName()).isEqualTo("Burger");
    }

    @Test
    void getCardPreview_cardNotFound_throwsResourceNotFoundException() {
        when(cardRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cardService.getCardPreview(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getCardPreview_vendorServiceThrows_throwsBadRequestException() {
        when(cardRepository.findById(1L)).thenReturn(Optional.of(card()));
        when(mappingRepository.findByCardDefinitionId(1L))
                .thenReturn(List.of(mapping(1L)));
        when(vendorServiceClient.getCategoriesWithItems(anyList()))
                .thenThrow(new RuntimeException("vendor-service down"));

        assertThatThrownBy(() -> cardService.getCardPreview(1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Unable to fetch menu details");
    }

    // ── bonusAmount calculation ───────────────────────────────────────────────

    @Test
    void getCardById_bonusAmountIsWalletMinusPrice() {
        CardDefinition c = card(); // price=500, wallet=600
        when(cardRepository.findById(1L)).thenReturn(Optional.of(c));
        when(mappingRepository.findByCardDefinitionId(1L)).thenReturn(List.of());

        CardDefinitionResponse response = cardService.getCardById(1L);

        assertThat(response.getBonusAmount()).isEqualByComparingTo("100.00");
    }

    @Test
    void getCardById_noBonusWhenPriceExceedsWallet_bonusIsZero() {
        CardDefinition c = card();
        c.setCardPrice(new BigDecimal("600.00"));
        c.setWalletAmount(new BigDecimal("500.00")); // price > wallet
        when(cardRepository.findById(1L)).thenReturn(Optional.of(c));
        when(mappingRepository.findByCardDefinitionId(1L)).thenReturn(List.of());

        CardDefinitionResponse response = cardService.getCardById(1L);

        assertThat(response.getBonusAmount()).isEqualByComparingTo("0.00");
    }
}