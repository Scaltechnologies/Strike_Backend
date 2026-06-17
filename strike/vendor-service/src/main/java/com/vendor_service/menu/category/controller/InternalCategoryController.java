package com.vendor_service.menu.category.controller;

import com.vendor_service.dto.response.CategoryWithItemsResponse;
import com.vendor_service.menu.category.entity.Category;
import com.vendor_service.menu.category.repository.CategoryRepository;
import com.vendor_service.menu.enums.CategoryStatus;
import com.vendor_service.menu.item.dto.response.MenuItemResponse;
import com.vendor_service.menu.item.entity.MenuItem;
import com.vendor_service.menu.item.mapper.MenuItemMapper;
import com.vendor_service.menu.item.repository.MenuItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/internal/categories")
@RequiredArgsConstructor
public class InternalCategoryController {

    private final CategoryRepository categoryRepository;
    private final MenuItemRepository menuItemRepository;
    private final MenuItemMapper menuItemMapper;

    /**
     * Returns the subset of the provided categoryIds that are ACTIVE and belong to the given store.
     * Used by card-service to validate category IDs during card creation/update.
     */
    @GetMapping("/validate")
    public List<Long> validateCategoryIds(
            @RequestParam List<Long> categoryIds,
            @RequestParam Long storeId) {
        return categoryRepository
                .findByIdInAndStoreIdAndStatus(categoryIds, storeId, CategoryStatus.ACTIVE)
                .stream()
                .map(Category::getId)
                .toList();
    }

    /**
     * Returns ACTIVE categories (with all their menu items) for the given IDs.
     * Used by card-service to build card previews and eligible menus.
     * Items are returned with their availabilityStatus so callers can filter as needed.
     * Uses a single batch query for items to avoid N+1.
     */
    @GetMapping("/by-ids")
    public List<CategoryWithItemsResponse> getCategoriesByIds(@RequestParam List<Long> ids) {
        // Only return ACTIVE categories — soft-deleted ones are excluded
        List<Category> categories = categoryRepository.findByIdIn(ids)
                .stream()
                .filter(c -> c.getStatus() == CategoryStatus.ACTIVE)
                .toList();

        if (categories.isEmpty()) return List.of();

        List<Long> activeCategoryIds = categories.stream().map(Category::getId).toList();

        // Single query for all items — avoids N+1
        Map<Long, List<MenuItem>> itemsByCategoryId = menuItemRepository
                .findByCategoryIdIn(activeCategoryIds)
                .stream()
                .collect(Collectors.groupingBy(MenuItem::getCategoryId));

        return categories.stream()
                .map(cat -> {
                    List<MenuItemResponse> items = itemsByCategoryId
                            .getOrDefault(cat.getId(), List.of())
                            .stream()
                            .map(menuItemMapper::toResponse)
                            .toList();
                    return CategoryWithItemsResponse.builder()
                            .id(cat.getId())
                            .name(cat.getName())
                            .description(cat.getDescription())
                            .imageUrl(cat.getImageUrl())
                            .displayOrder(cat.getDisplayOrder())
                            .items(items)
                            .build();
                })
                .toList();
    }
}