package com.vendor_service.menu.item.service;

import com.vendor_service.menu.item.dto.request.CreateMenuItemRequest;
import com.vendor_service.menu.item.dto.request.UpdateMenuItemRequest;
import com.vendor_service.menu.item.dto.response.MenuItemResponse;

import java.util.List;

public interface MenuItemService {

    MenuItemResponse createMenuItem(Long vendorId, CreateMenuItemRequest request);

    List<MenuItemResponse> getMyMenuItems(Long vendorId);

    MenuItemResponse getMenuItemById(Long vendorId, Long itemId);

    List<MenuItemResponse> getItemsByCategory(Long vendorId, Long categoryId);

    MenuItemResponse updateMenuItem(Long vendorId, Long itemId, UpdateMenuItemRequest request);

    void deleteMenuItem(Long vendorId, Long itemId);

    List<MenuItemResponse> getItemsByStore(Long storeId);
}