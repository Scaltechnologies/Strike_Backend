package com.card_service.common.repository;

import com.card_service.common.entity.CardMenuItemMapping;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CardMenuItemMappingRepository extends JpaRepository<CardMenuItemMapping, Long> {

    List<CardMenuItemMapping> findByCardDefinitionId(Long cardDefinitionId);

    List<CardMenuItemMapping> findByCardDefinitionIdIn(List<Long> cardDefinitionIds);

    void deleteByCardDefinitionId(Long cardDefinitionId);
}