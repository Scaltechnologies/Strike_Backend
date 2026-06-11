package com.user_service.store.controller;

import com.user_service.profile.entity.UserProfile;
import com.user_service.profile.repository.UserProfileRepository;
import com.user_service.store.client.StoreServiceClient;
import com.user_service.store.dto.MenuCategoryResponse;
import com.user_service.store.dto.StoreResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/api/user/stores")
@RequiredArgsConstructor
public class UserStoreController {

    private final StoreServiceClient storeServiceClient;
    private final UserProfileRepository userProfileRepository;

    @GetMapping
    public List<StoreResponse> getAllStores() {
        return storeServiceClient.getAllActiveStores();
    }

    /**
     * Find stores near an explicit location.
     * Usage: GET /api/user/stores/nearby?lat=12.97&lng=77.59&radiusKm=5
     */
    @GetMapping("/nearby")
    public List<StoreResponse> getNearbyStores(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "5.0") double radiusKm) {
        return storeServiceClient.getNearbyStores(lat, lng, radiusKm);
    }

    /**
     * Find stores near the authenticated user's saved location.
     * Requires the user to have previously called PATCH /api/user/me/location.
     * Usage: GET /api/user/stores/nearby/me?radiusKm=5
     */
    @GetMapping("/nearby/me")
    public List<StoreResponse> getNearbyStoresForMe(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(defaultValue = "5.0") double radiusKm) {
        UserProfile profile = userProfileRepository.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Profile not found"));
        if (profile.getLatitude() == null || profile.getLongitude() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "No saved location found. Please update your location via PATCH /api/user/me/location first.");
        }
        return storeServiceClient.getNearbyStores(profile.getLatitude(), profile.getLongitude(), radiusKm);
    }

    /**
     * Search stores by name or category keyword.
     * Usage: GET /api/user/stores/search?q=pizza&category=restaurant
     */
    @GetMapping("/search")
    public List<StoreResponse> searchStores(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category) {
        return storeServiceClient.searchStores(q, category);
    }

    @GetMapping("/{storeId}")
    public StoreResponse getStore(@PathVariable Long storeId) {
        StoreResponse store = storeServiceClient.getStoreById(storeId);
        if (store == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Store not found: " + storeId);
        return store;
    }

    @GetMapping("/{storeId}/menu")
    public List<MenuCategoryResponse> getStoreMenu(@PathVariable Long storeId) {
        return storeServiceClient.getStoreMenu(storeId);
    }
}