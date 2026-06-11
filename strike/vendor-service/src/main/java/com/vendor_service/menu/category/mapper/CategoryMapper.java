package com.vendor_service.menu.category.mapper;

import com.vendor_service.menu.category.dto.request.CreateCategoryRequest;
import com.vendor_service.menu.category.dto.response.CategoryResponse;
import com.vendor_service.menu.category.entity.Category;
import com.vendor_service.menu.enums.CategoryStatus;

public class CategoryMapper {

    private CategoryMapper() {
    }

    public static Category toEntity(CreateCategoryRequest request, Long storeId) {

        return Category.builder()
                .name(request.getName())
                .description(request.getDescription())
                .imageUrl(request.getImageUrl())
                .displayOrder(request.getDisplayOrder())
                .storeId(storeId)
                .status(CategoryStatus.ACTIVE)
                .build();
    }

    public static CategoryResponse toResponse(Category category) {

        return CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .description(category.getDescription())
                .imageUrl(category.getImageUrl())
                .displayOrder(category.getDisplayOrder())
                .status(category.getStatus())
                .storeId(category.getStoreId())
                .build();
    }
}