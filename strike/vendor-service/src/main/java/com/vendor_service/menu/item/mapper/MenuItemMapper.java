package com.vendor_service.menu.item.mapper;

import com.vendor_service.menu.item.dto.request.CreateMenuItemRequest;
import com.vendor_service.menu.item.dto.request.UpdateMenuItemRequest;
import com.vendor_service.menu.item.dto.response.MenuItemResponse;
import com.vendor_service.menu.item.entity.MenuItem;
import org.springframework.stereotype.Component;

@Component
public class MenuItemMapper {

    public MenuItem toEntity(CreateMenuItemRequest request, Long storeId) {

        return MenuItem.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .imageUrl(request.getImageUrl())
                .itemType(request.getItemType())
                .availabilityStatus(request.getAvailabilityStatus())
                .categoryId(request.getCategoryId())
                .storeId(storeId)
                .build();
    }

    public void updateEntity(MenuItem menuItem, UpdateMenuItemRequest request) {
        if (request.getName()               != null) menuItem.setName(request.getName());
        if (request.getDescription()        != null) menuItem.setDescription(request.getDescription());
        if (request.getPrice()              != null) menuItem.setPrice(request.getPrice());
        if (request.getImageUrl()           != null) menuItem.setImageUrl(request.getImageUrl());
        if (request.getItemType()           != null) menuItem.setItemType(request.getItemType());
        if (request.getAvailabilityStatus() != null) menuItem.setAvailabilityStatus(request.getAvailabilityStatus());
        if (request.getCategoryId()         != null) menuItem.setCategoryId(request.getCategoryId());
    }

    public MenuItemResponse toResponse(MenuItem menuItem) {

        return MenuItemResponse.builder()
                .id(menuItem.getId())
                .name(menuItem.getName())
                .description(menuItem.getDescription())
                .price(menuItem.getPrice())
                .imageUrl(menuItem.getImageUrl())
                .itemType(menuItem.getItemType())
                .availabilityStatus(menuItem.getAvailabilityStatus())
                .categoryId(menuItem.getCategoryId())
                .storeId(menuItem.getStoreId())
                .createdAt(menuItem.getCreatedAt())
                .updatedAt(menuItem.getUpdatedAt())
                .build();
    }
}