package com.vendor_service.menu.item.controller;

import com.vendor_service.common.annotation.CurrentVendorId;
import com.vendor_service.common.response.ApiResponse;
import com.vendor_service.menu.item.dto.request.CreateMenuItemRequest;
import com.vendor_service.menu.item.dto.request.UpdateMenuItemRequest;
import com.vendor_service.menu.item.dto.response.MenuItemResponse;
import com.vendor_service.menu.item.service.MenuItemService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/menu/items")
@RequiredArgsConstructor
@PreAuthorize("hasRole('VENDOR')")
public class MenuItemController {

    private final MenuItemService menuItemService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<MenuItemResponse> createMenuItem(
            @CurrentVendorId Long vendorId,
            @Valid @RequestBody CreateMenuItemRequest request
    ) {
        return ApiResponse.success("Item created successfully", menuItemService.createMenuItem(vendorId, request));
    }

    @GetMapping
    public ApiResponse<List<MenuItemResponse>> getMyMenuItems(
            @CurrentVendorId Long vendorId
    ) {
        return ApiResponse.success(menuItemService.getMyMenuItems(vendorId));
    }

    @GetMapping("/{itemId}")
    public ApiResponse<MenuItemResponse> getMenuItemById(
            @CurrentVendorId Long vendorId,
            @PathVariable Long itemId
    ) {
        return ApiResponse.success(menuItemService.getMenuItemById(vendorId, itemId));
    }

    @GetMapping("/by-category/{categoryId}")
    public ApiResponse<List<MenuItemResponse>> getItemsByCategory(
            @CurrentVendorId Long vendorId,
            @PathVariable Long categoryId
    ) {
        return ApiResponse.success(menuItemService.getItemsByCategory(vendorId, categoryId));
    }

    @PutMapping("/{itemId}")
    public ApiResponse<MenuItemResponse> updateMenuItem(
            @CurrentVendorId Long vendorId,
            @PathVariable Long itemId,
            @Valid @RequestBody UpdateMenuItemRequest request
    ) {
        return ApiResponse.success("Item updated successfully", menuItemService.updateMenuItem(vendorId, itemId, request));
    }

    @DeleteMapping("/{itemId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMenuItem(
            @CurrentVendorId Long vendorId,
            @PathVariable Long itemId
    ) {
        menuItemService.deleteMenuItem(vendorId, itemId);
    }
}