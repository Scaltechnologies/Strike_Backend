package com.vendor_service.menu.item.service.impl;

import com.vendor_service.common.exception.BadRequestException;
import com.vendor_service.common.exception.ResourceNotFoundException;
import com.vendor_service.menu.category.entity.Category;
import com.vendor_service.menu.category.repository.CategoryRepository;
import com.vendor_service.menu.enums.CategoryStatus;
import com.vendor_service.menu.item.dto.request.CreateMenuItemRequest;
import com.vendor_service.menu.item.dto.request.UpdateMenuItemRequest;
import com.vendor_service.menu.item.dto.response.MenuItemResponse;
import com.vendor_service.menu.item.entity.MenuItem;
import com.vendor_service.menu.item.mapper.MenuItemMapper;
import com.vendor_service.menu.item.repository.MenuItemRepository;
import com.vendor_service.menu.item.service.MenuItemService;
import com.vendor_service.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class MenuItemServiceImpl implements MenuItemService {

    private final MenuItemRepository menuItemRepository;
    private final CategoryRepository categoryRepository;
    private final StoreRepository storeRepository;
    private final MenuItemMapper menuItemMapper;

    @Override
    public MenuItemResponse createMenuItem(Long vendorId, CreateMenuItemRequest request) {
        Long storeId = resolveStoreId(vendorId);
        validateCategoryOwnership(request.getCategoryId(), storeId);

        MenuItem menuItem = menuItemMapper.toEntity(request, storeId);
        return menuItemMapper.toResponse(menuItemRepository.save(menuItem));
    }

    @Override
    public List<MenuItemResponse> getMyMenuItems(Long vendorId) {
        Long storeId = resolveStoreId(vendorId);
        return menuItemRepository.findByStoreId(storeId)
                .stream().map(menuItemMapper::toResponse).toList();
    }

    @Override
    public MenuItemResponse getMenuItemById(Long vendorId, Long itemId) {
        Long storeId = resolveStoreId(vendorId);
        MenuItem item = findByIdAndStore(itemId, storeId);
        return menuItemMapper.toResponse(item);
    }

    @Override
    public List<MenuItemResponse> getItemsByCategory(Long vendorId, Long categoryId) {
        Long storeId = resolveStoreId(vendorId);
        validateCategoryOwnership(categoryId, storeId);
        return menuItemRepository.findByCategoryIdAndStoreId(categoryId, storeId)
                .stream().map(menuItemMapper::toResponse).toList();
    }

    @Override
    public MenuItemResponse updateMenuItem(Long vendorId, Long itemId, UpdateMenuItemRequest request) {
        Long storeId = resolveStoreId(vendorId);
        MenuItem item = findByIdAndStore(itemId, storeId);

        if (request.getCategoryId() != null) {
            validateCategoryOwnership(request.getCategoryId(), storeId);
            item.setCategoryId(request.getCategoryId());
        }

        menuItemMapper.updateEntity(item, request);
        return menuItemMapper.toResponse(menuItemRepository.save(item));
    }

    @Override
    public void deleteMenuItem(Long vendorId, Long itemId) {
        Long storeId = resolveStoreId(vendorId);
        MenuItem item = findByIdAndStore(itemId, storeId);
        menuItemRepository.delete(item);
    }

    @Override
    public List<MenuItemResponse> getItemsByStore(Long storeId) {
        return menuItemRepository.findByStoreId(storeId)
                .stream().map(menuItemMapper::toResponse).toList();
    }

    private Long resolveStoreId(Long vendorId) {
        return storeRepository.findByVendorId(vendorId)
                .stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Store not found for vendor: " + vendorId))
                .getId();
    }

    private MenuItem findByIdAndStore(Long itemId, Long storeId) {
        return menuItemRepository.findByIdAndStoreId(itemId, storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Menu item not found: " + itemId));
    }

    private void validateCategoryOwnership(Long categoryId, Long storeId) {
        Category category = categoryRepository.findByIdAndStoreId(categoryId, storeId)
                .orElseThrow(() -> new BadRequestException("Category not found in your store: " + categoryId));
        if (category.getStatus() == CategoryStatus.INACTIVE) {
            throw new BadRequestException("Category is inactive: " + categoryId);
        }
    }
}