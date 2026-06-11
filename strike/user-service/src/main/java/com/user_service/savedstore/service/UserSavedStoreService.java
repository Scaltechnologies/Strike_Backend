package com.user_service.savedstore.service;

import com.user_service.savedstore.dto.SavedStoreResponse;

import java.util.List;

public interface UserSavedStoreService {
    List<SavedStoreResponse> getSavedStores(Long userId);
    SavedStoreResponse saveStore(Long userId, Long storeId);
    void removeStore(Long userId, Long storeId);
    boolean isStoreSaved(Long userId, Long storeId);
}