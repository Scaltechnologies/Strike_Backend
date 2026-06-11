package com.vendor_service.service;

import com.vendor_service.common.enums.StoreStatus;
import com.vendor_service.dto.request.StoreDetailsRequest;
import com.vendor_service.dto.response.CategoryWithItemsResponse;
import com.vendor_service.dto.response.StoreResponse;

import java.util.List;

public interface StoreService {

    StoreResponse getStoreByVendorId(Long vendorId);

    StoreResponse updateStoreByVendorId(Long vendorId, StoreDetailsRequest request);

    StoreResponse updateStoreLocation(Long vendorId, double latitude, double longitude);

    StoreResponse updateStoreStatusByVendorId(Long vendorId, StoreStatus status);

    List<StoreResponse> getAllActiveStores();

    List<StoreResponse> getNearbyStores(double lat, double lng, double radiusKm);

    List<StoreResponse> searchStores(String query, String category);

    StoreResponse getStoreById(Long storeId);

    List<CategoryWithItemsResponse> getMenuByStoreId(Long storeId);

    void validateStoreOwnership(Long storeId, Long vendorId);
}