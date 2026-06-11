package com.vendor_service.menu.category.service.impl;

import com.vendor_service.common.exception.ResourceNotFoundException;
import com.vendor_service.menu.category.dto.request.CreateCategoryRequest;
import com.vendor_service.menu.category.dto.request.UpdateCategoryRequest;
import com.vendor_service.menu.category.dto.response.CategoryResponse;
import com.vendor_service.menu.category.entity.Category;
import com.vendor_service.menu.category.mapper.CategoryMapper;
import com.vendor_service.menu.category.repository.CategoryRepository;
import com.vendor_service.menu.category.service.CategoryService;
import com.vendor_service.menu.enums.CategoryStatus;
import com.vendor_service.repository.StoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;
    private final StoreRepository storeRepository;

    @Override
    public CategoryResponse createCategory(Long vendorId, CreateCategoryRequest request) {
        Long storeId = resolveStoreId(vendorId);
        Category category = CategoryMapper.toEntity(request, storeId);
        return CategoryMapper.toResponse(categoryRepository.save(category));
    }

    @Override
    public List<CategoryResponse> getMyCategories(Long vendorId) {
        Long storeId = resolveStoreId(vendorId);
        return categoryRepository.findByStoreIdAndStatus(storeId, CategoryStatus.ACTIVE)
                .stream()
                .map(CategoryMapper::toResponse)
                .toList();
    }

    @Override
    public CategoryResponse getCategoryById(Long vendorId, Long categoryId) {
        Long storeId = resolveStoreId(vendorId);
        Category category = findByIdAndStore(categoryId, storeId);
        return CategoryMapper.toResponse(category);
    }

    @Override
    public CategoryResponse updateCategory(Long vendorId, Long categoryId, UpdateCategoryRequest request) {
        Long storeId = resolveStoreId(vendorId);
        Category category = findByIdAndStore(categoryId, storeId);

        if (request.getName() != null) category.setName(request.getName());
        if (request.getDescription() != null) category.setDescription(request.getDescription());
        if (request.getImageUrl() != null) category.setImageUrl(request.getImageUrl());
        if (request.getDisplayOrder() != null) category.setDisplayOrder(request.getDisplayOrder());

        return CategoryMapper.toResponse(categoryRepository.save(category));
    }

    @Override
    public void deleteCategory(Long vendorId, Long categoryId) {
        Long storeId = resolveStoreId(vendorId);
        Category category = findByIdAndStore(categoryId, storeId);
        category.setStatus(CategoryStatus.INACTIVE);
        categoryRepository.save(category);
    }

    private Long resolveStoreId(Long vendorId) {
        return storeRepository.findByVendorId(vendorId)
                .stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Store not found for vendor: " + vendorId))
                .getId();
    }

    private Category findByIdAndStore(Long categoryId, Long storeId) {
        return categoryRepository.findByIdAndStoreId(categoryId, storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Category not found: " + categoryId));
    }
}