package com.user_service.savedstore.controller;

import com.user_service.savedstore.dto.SavedStoreResponse;
import com.user_service.savedstore.service.UserSavedStoreService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user/me/saved-stores")
@RequiredArgsConstructor
public class UserSavedStoreController {

    private final UserSavedStoreService userSavedStoreService;

    @GetMapping
    public List<SavedStoreResponse> getSavedStores(
            @RequestHeader("X-User-Id") Long userId) {
        return userSavedStoreService.getSavedStores(userId);
    }

    @PostMapping("/{storeId}")
    @ResponseStatus(HttpStatus.CREATED)
    public SavedStoreResponse saveStore(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long storeId) {
        return userSavedStoreService.saveStore(userId, storeId);
    }

    @DeleteMapping("/{storeId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void removeStore(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long storeId) {
        userSavedStoreService.removeStore(userId, storeId);
    }

    @GetMapping("/{storeId}/check")
    public Map<String, Boolean> checkIfSaved(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long storeId) {
        return Map.of("saved", userSavedStoreService.isStoreSaved(userId, storeId));
    }
}