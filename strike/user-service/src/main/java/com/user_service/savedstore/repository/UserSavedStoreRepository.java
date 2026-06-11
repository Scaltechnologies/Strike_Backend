package com.user_service.savedstore.repository;

import com.user_service.savedstore.entity.UserSavedStore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserSavedStoreRepository extends JpaRepository<UserSavedStore, Long> {
    List<UserSavedStore> findByUserId(Long userId);
    Optional<UserSavedStore> findByUserIdAndStoreId(Long userId, Long storeId);
    boolean existsByUserIdAndStoreId(Long userId, Long storeId);
    void deleteByUserIdAndStoreId(Long userId, Long storeId);
}