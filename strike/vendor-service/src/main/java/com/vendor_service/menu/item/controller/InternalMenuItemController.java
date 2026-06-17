package com.vendor_service.menu.item.controller;

import com.vendor_service.menu.item.dto.response.MenuItemResponse;
import com.vendor_service.menu.item.entity.MenuItem;
import com.vendor_service.menu.item.mapper.MenuItemMapper;
import com.vendor_service.menu.item.repository.MenuItemRepository;
import com.vendor_service.menu.item.service.MenuItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/internal/menu-items")
@RequiredArgsConstructor
public class InternalMenuItemController {

    private final MenuItemService menuItemService;
    private final MenuItemRepository menuItemRepository;
    private final MenuItemMapper menuItemMapper;

    @GetMapping("/store/{storeId}")
    public List<MenuItemResponse> getItemsByStore(@PathVariable Long storeId) {
        return menuItemService.getItemsByStore(storeId);
    }

    /**
     * Returns the subset of menuItemIds that are valid — they must belong to the given store
     * and, when categoryIds is provided, also belong to one of those categories.
     * Used by card-service to validate eligibleMenuItemIds during card create/update.
     */
    @GetMapping("/validate")
    public List<Long> validateMenuItemIds(
            @RequestParam List<Long> menuItemIds,
            @RequestParam Long storeId,
            @RequestParam(required = false) List<Long> categoryIds) {

        List<MenuItem> items;
        if (categoryIds != null && !categoryIds.isEmpty()) {
            items = menuItemRepository.findByIdInAndStoreIdAndCategoryIdIn(menuItemIds, storeId, categoryIds);
        } else {
            items = menuItemRepository.findByIdInAndStoreId(menuItemIds, storeId);
        }
        return items.stream().map(MenuItem::getId).toList();
    }

    /**
     * Returns MenuItemResponse for each of the given IDs.
     * Used by card-service to enrich card responses with item names and prices.
     */
    @GetMapping("/by-ids")
    public List<MenuItemResponse> getMenuItemsByIds(@RequestParam List<Long> ids) {
        return menuItemRepository.findByIdIn(ids).stream()
                .map(menuItemMapper::toResponse)
                .toList();
    }
}