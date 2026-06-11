package com.vendor_service.menu.category.service;

import com.vendor_service.menu.category.dto.request.CreateCategoryRequest;
import com.vendor_service.menu.category.dto.request.UpdateCategoryRequest;
import com.vendor_service.menu.category.dto.response.CategoryResponse;

import java.util.List;

public interface CategoryService {

    CategoryResponse createCategory(Long vendorId, CreateCategoryRequest request);

    List<CategoryResponse> getMyCategories(Long vendorId);

    CategoryResponse getCategoryById(Long vendorId, Long categoryId);

    CategoryResponse updateCategory(Long vendorId, Long categoryId, UpdateCategoryRequest request);

    void deleteCategory(Long vendorId, Long categoryId);
}