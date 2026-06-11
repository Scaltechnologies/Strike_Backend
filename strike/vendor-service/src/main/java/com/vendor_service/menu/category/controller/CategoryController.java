package com.vendor_service.menu.category.controller;

import com.vendor_service.common.annotation.CurrentVendorId;
import com.vendor_service.common.response.ApiResponse;
import com.vendor_service.menu.category.dto.request.CreateCategoryRequest;
import com.vendor_service.menu.category.dto.request.UpdateCategoryRequest;
import com.vendor_service.menu.category.dto.response.CategoryResponse;
import com.vendor_service.menu.category.service.CategoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/menu/categories")
@RequiredArgsConstructor
@PreAuthorize("hasRole('VENDOR')")
public class CategoryController {

    private final CategoryService categoryService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<CategoryResponse> createCategory(
            @CurrentVendorId Long vendorId,
            @Valid @RequestBody CreateCategoryRequest request
    ) {
        return ApiResponse.success("Category created successfully", categoryService.createCategory(vendorId, request));
    }

    @GetMapping
    public ApiResponse<List<CategoryResponse>> getMyCategories(
            @CurrentVendorId Long vendorId
    ) {
        return ApiResponse.success(categoryService.getMyCategories(vendorId));
    }

    @GetMapping("/{categoryId}")
    public ApiResponse<CategoryResponse> getCategoryById(
            @CurrentVendorId Long vendorId,
            @PathVariable Long categoryId
    ) {
        return ApiResponse.success(categoryService.getCategoryById(vendorId, categoryId));
    }

    @PutMapping("/{categoryId}")
    public ApiResponse<CategoryResponse> updateCategory(
            @CurrentVendorId Long vendorId,
            @PathVariable Long categoryId,
            @Valid @RequestBody UpdateCategoryRequest request
    ) {
        return ApiResponse.success("Category updated successfully", categoryService.updateCategory(vendorId, categoryId, request));
    }

    @DeleteMapping("/{categoryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteCategory(
            @CurrentVendorId Long vendorId,
            @PathVariable Long categoryId
    ) {
        categoryService.deleteCategory(vendorId, categoryId);
    }
}