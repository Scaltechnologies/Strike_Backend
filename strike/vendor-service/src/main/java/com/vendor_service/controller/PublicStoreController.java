package com.vendor_service.controller;

import com.vendor_service.common.response.ApiResponse;
import com.vendor_service.dto.response.CategoryWithItemsResponse;
import com.vendor_service.dto.response.StoreResponse;
import com.vendor_service.service.StoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/stores")
@RequiredArgsConstructor
public class PublicStoreController {

    private final StoreService storeService;

    @GetMapping
    public ApiResponse<List<StoreResponse>> getAllStores() {
        return ApiResponse.success(storeService.getAllActiveStores());
    }

    @GetMapping("/nearby")
    public ApiResponse<List<StoreResponse>> getNearbyStores(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "5.0") double radiusKm) {
        return ApiResponse.success(storeService.getNearbyStores(lat, lng, radiusKm));
    }

    @GetMapping("/search")
    public ApiResponse<List<StoreResponse>> searchStores(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category) {
        return ApiResponse.success(storeService.searchStores(q, category));
    }

    @GetMapping("/{storeId}")
    public ApiResponse<StoreResponse> getStore(@PathVariable Long storeId) {
        return ApiResponse.success(storeService.getStoreById(storeId));
    }

    @GetMapping("/{storeId}/menu")
    public ApiResponse<List<CategoryWithItemsResponse>> getMenu(@PathVariable Long storeId) {
        return ApiResponse.success(storeService.getMenuByStoreId(storeId));
    }
}