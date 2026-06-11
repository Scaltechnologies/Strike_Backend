package com.card_service.common.repository;

import com.card_service.common.entity.CardDefinition;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CardDefinitionRepository extends JpaRepository<CardDefinition, Long> {
    List<CardDefinition> findByStoreId(Long storeId);
    List<CardDefinition> findByStoreIdAndIsActiveTrue(Long storeId);
    List<CardDefinition> findByVendorId(Long vendorId);
}