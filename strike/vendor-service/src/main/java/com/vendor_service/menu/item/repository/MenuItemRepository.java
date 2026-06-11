package com.vendor_service.menu.item.repository;

import com.vendor_service.menu.item.entity.MenuItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MenuItemRepository extends JpaRepository<MenuItem, Long> {

    List<MenuItem> findByCategoryIdAndStoreId(Long categoryId, Long storeId);
    List<MenuItem> findByStoreId(Long storeId);
    Optional<MenuItem> findByIdAndStoreId(Long id, Long storeId);
}