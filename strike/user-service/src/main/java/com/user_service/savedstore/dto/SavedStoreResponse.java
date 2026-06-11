package com.user_service.savedstore.dto;

import com.user_service.store.dto.StoreResponse;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class SavedStoreResponse {
    private Long id;
    private Long storeId;
    private LocalDateTime savedAt;
    private StoreResponse storeDetails;
}