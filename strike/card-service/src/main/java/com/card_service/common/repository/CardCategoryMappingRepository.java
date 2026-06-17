package com.card_service.common.repository;

import com.card_service.common.entity.CardCategoryMapping;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface CardCategoryMappingRepository extends JpaRepository<CardCategoryMapping, Long> {

    List<CardCategoryMapping> findByCardDefinitionId(Long cardDefinitionId);

    List<CardCategoryMapping> findByCardDefinitionIdIn(List<Long> cardDefinitionIds);

    List<CardCategoryMapping> findByCategoryId(Long categoryId);

    void deleteByCardDefinitionId(Long cardDefinitionId);

    @Query("SELECT CASE WHEN COUNT(m) > 0 THEN true ELSE false END " +
           "FROM CardCategoryMapping m, CardDefinition c " +
           "WHERE m.categoryId = :categoryId " +
           "AND m.cardDefinitionId = c.id " +
           "AND c.isActive = true")
    boolean isMappedToActiveCard(@Param("categoryId") Long categoryId);
}