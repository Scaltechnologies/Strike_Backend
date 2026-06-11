package com.user_service.savedstore.service.impl;

import com.user_service.savedstore.dto.SavedStoreResponse;
import com.user_service.savedstore.entity.UserSavedStore;
import com.user_service.savedstore.repository.UserSavedStoreRepository;
import com.user_service.savedstore.service.UserSavedStoreService;
import com.user_service.store.client.StoreServiceClient;
import com.user_service.store.dto.StoreResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserSavedStoreServiceImpl implements UserSavedStoreService {

    private final UserSavedStoreRepository userSavedStoreRepository;
    private final StoreServiceClient storeServiceClient;

    @Override
    public List<SavedStoreResponse> getSavedStores(Long userId) {
        return userSavedStoreRepository.findByUserId(userId)
                .stream()
                .map(saved -> SavedStoreResponse.builder()
                        .id(saved.getId())
                        .storeId(saved.getStoreId())
                        .savedAt(saved.getSavedAt())
                        .storeDetails(storeServiceClient.getStoreById(saved.getStoreId()))
                        .build())
                .toList();
    }

    @Override
    public SavedStoreResponse saveStore(Long userId, Long storeId) {
        if (userSavedStoreRepository.existsByUserIdAndStoreId(userId, storeId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Store already saved");
        }
        StoreResponse storeDetails = storeServiceClient.getStoreById(storeId);
        if (storeDetails == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Store not found: " + storeId);
        }
        UserSavedStore saved = userSavedStoreRepository.save(
                UserSavedStore.builder().userId(userId).storeId(storeId).build());
        return SavedStoreResponse.builder()
                .id(saved.getId())
                .storeId(saved.getStoreId())
                .savedAt(saved.getSavedAt())
                .storeDetails(storeDetails)
                .build();
    }

    @Override
    @Transactional
    public void removeStore(Long userId, Long storeId) {
        if (!userSavedStoreRepository.existsByUserIdAndStoreId(userId, storeId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Saved store not found");
        }
        userSavedStoreRepository.deleteByUserIdAndStoreId(userId, storeId);
    }

    @Override
    public boolean isStoreSaved(Long userId, Long storeId) {
        return userSavedStoreRepository.existsByUserIdAndStoreId(userId, storeId);
    }
}