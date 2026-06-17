package com.vendor_service.menu.category.repository;

import com.vendor_service.menu.category.entity.Category;
import com.vendor_service.menu.enums.CategoryStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    List<Category> findByStoreIdAndStatus(Long storeId, CategoryStatus status);

    Optional<Category> findByIdAndStoreId(Long id, Long storeId);

    List<Category> findByIdInAndStoreIdAndStatus(List<Long> ids, Long storeId, CategoryStatus status);

    List<Category> findByIdIn(List<Long> ids);
}