package com.vendor_service.service.impl;

import com.vendor_service.common.enums.StoreStatus;
import com.vendor_service.common.exception.ResourceNotFoundException;
import com.vendor_service.common.exception.UnauthorizedException;
import com.vendor_service.dto.request.StoreDetailsRequest;
import com.vendor_service.dto.response.CategoryWithItemsResponse;
import com.vendor_service.dto.response.StoreHolidayResponse;
import com.vendor_service.dto.response.StoreResponse;
import com.vendor_service.dto.response.StoreTimingResponse;
import com.vendor_service.entity.Store;
import com.vendor_service.entity.StoreHoliday;
import com.vendor_service.entity.StoreTiming;
import com.vendor_service.menu.category.repository.CategoryRepository;
import com.vendor_service.menu.enums.CategoryStatus;
import com.vendor_service.menu.item.mapper.MenuItemMapper;
import com.vendor_service.menu.item.repository.MenuItemRepository;
import com.vendor_service.repository.StoreHolidayRepository;
import com.vendor_service.repository.StoreRepository;
import com.vendor_service.repository.StoreTimingRepository;
import com.vendor_service.service.StoreService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class StoreServiceImpl implements StoreService {

    private final StoreRepository storeRepository;
    private final StoreTimingRepository storeTimingRepository;
    private final StoreHolidayRepository storeHolidayRepository;
    private final CategoryRepository categoryRepository;
    private final MenuItemRepository menuItemRepository;
    private final MenuItemMapper menuItemMapper;

    @Override
    public StoreResponse getStoreByVendorId(Long vendorId) {
        log.info("[StoreService] getStoreByVendorId — vendorId={}", vendorId);
        Store store = findByVendorId(vendorId);
        log.info("[StoreService] found store — id={}, name={}", store.getId(), store.getName());
        StoreResponse response = toResponse(store);
        log.info("[StoreService] DTO mapping complete — storeId={}", response.getId());
        return response;
    }

    @Override
    public StoreResponse updateStoreByVendorId(Long vendorId, StoreDetailsRequest request) {
        Store store = findByVendorId(vendorId);
        store.setName(request.getName());
        store.setAddress(request.getAddress());
        store.setPhone(request.getPhone());
        store.setEmail(request.getEmail());
        store.setCategory(request.getCategory());
        store.setDescription(request.getDescription());
        store.setLogoUrl(request.getLogoUrl());
        if (request.getLatitude() != null) store.setLatitude(request.getLatitude());
        if (request.getLongitude() != null) store.setLongitude(request.getLongitude());
        return toResponse(storeRepository.save(store));
    }

    @Override
    public StoreResponse updateStoreLocation(Long vendorId, double latitude, double longitude) {
        Store store = findByVendorId(vendorId);
        store.setLatitude(latitude);
        store.setLongitude(longitude);
        return toResponse(storeRepository.save(store));
    }

    @Override
    public StoreResponse updateStoreStatusByVendorId(Long vendorId, StoreStatus status) {
        Store store = findByVendorId(vendorId);
        store.setStatus(status);
        return toResponse(storeRepository.save(store));
    }

    @Override
    public List<StoreResponse> getAllActiveStores() {
        return storeRepository.findAllByStatus(StoreStatus.ACTIVE)
                .stream().map(this::toResponse).toList();
    }

    @Override
    public List<StoreResponse> getNearbyStores(double lat, double lng, double radiusKm) {
        return storeRepository.findAllByStatus(StoreStatus.ACTIVE)
                .stream()
                .filter(s -> s.getLatitude() != null && s.getLongitude() != null)
                .map(s -> {
                    double distance = haversineKm(lat, lng, s.getLatitude(), s.getLongitude());
                    StoreResponse response = toResponse(s);
                    response.setDistanceKm(Math.round(distance * 100.0) / 100.0);
                    return response;
                })
                .filter(r -> r.getDistanceKm() <= radiusKm)
                .sorted(Comparator.comparingDouble(StoreResponse::getDistanceKm))
                .toList();
    }

    @Override
    public List<StoreResponse> searchStores(String query, String category) {
        String q = (query != null) ? query.trim().toLowerCase() : "";
        String cat = (category != null) ? category.trim().toLowerCase() : "";
        return storeRepository.findAllByStatus(StoreStatus.ACTIVE)
                .stream()
                .filter(s -> {
                    boolean matchesQ = q.isEmpty()
                            || s.getName().toLowerCase().contains(q)
                            || (s.getDescription() != null && s.getDescription().toLowerCase().contains(q));
                    boolean matchesCat = cat.isEmpty()
                            || (s.getCategory() != null && s.getCategory().toLowerCase().contains(cat));
                    return matchesQ && matchesCat;
                })
                .map(this::toResponse)
                .toList();
    }

    @Override
    public StoreResponse getStoreById(Long storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found: " + storeId));
        return toResponse(store);
    }

    @Override
    public List<CategoryWithItemsResponse> getMenuByStoreId(Long storeId) {
        storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found: " + storeId));

        return categoryRepository.findByStoreIdAndStatus(storeId, CategoryStatus.ACTIVE)
                .stream()
                .sorted((a, b) -> {
                    Integer ao = a.getDisplayOrder() != null ? a.getDisplayOrder() : Integer.MAX_VALUE;
                    Integer bo = b.getDisplayOrder() != null ? b.getDisplayOrder() : Integer.MAX_VALUE;
                    return ao.compareTo(bo);
                })
                .map(cat -> CategoryWithItemsResponse.builder()
                        .id(cat.getId())
                        .name(cat.getName())
                        .description(cat.getDescription())
                        .imageUrl(cat.getImageUrl())
                        .displayOrder(cat.getDisplayOrder())
                        .items(menuItemRepository.findByCategoryIdAndStoreId(cat.getId(), storeId)
                                .stream().map(menuItemMapper::toResponse).toList())
                        .build())
                .toList();
    }

    @Override
    public void validateStoreOwnership(Long storeId, Long vendorId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new ResourceNotFoundException("Store not found: " + storeId));
        if (!store.getVendorId().equals(vendorId)) {
            throw new UnauthorizedException("You do not own this store");
        }
    }

    private Store findByVendorId(Long vendorId) {
        List<Store> results = storeRepository.findByVendorId(vendorId);
        log.info("[StoreService] findByVendorId — vendorId={}, found={}", vendorId, results.size());
        return results.stream().findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Store not found for vendor: " + vendorId));
    }

    private StoreResponse toResponse(Store store) {
        List<StoreTimingResponse> timings = storeTimingRepository.findByStoreId(store.getId())
                .stream()
                .map(this::toTimingResponse)
                .toList();

        List<StoreHolidayResponse> holidays = storeHolidayRepository.findByStoreId(store.getId())
                .stream()
                .map(this::toHolidayResponse)
                .toList();

        return StoreResponse.builder()
                .id(store.getId())
                .vendorId(store.getVendorId())
                .name(store.getName())
                .address(store.getAddress())
                .phone(store.getPhone())
                .email(store.getEmail())
                .category(store.getCategory())
                .description(store.getDescription())
                .logoUrl(store.getLogoUrl())
                .latitude(store.getLatitude())
                .longitude(store.getLongitude())
                .status(store.getStatus())
                .timings(timings)
                .holidays(holidays)
                .createdAt(store.getCreatedAt())
                .updatedAt(store.getUpdatedAt())
                .build();
    }

    private double haversineKm(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private StoreTimingResponse toTimingResponse(StoreTiming timing) {
        return StoreTimingResponse.builder()
                .id(timing.getId())
                .storeId(timing.getStoreId())
                .dayOfWeek(timing.getDayOfWeek())
                .openTime(timing.getOpenTime())
                .closeTime(timing.getCloseTime())
                .isClosed(timing.getIsClosed())
                .build();
    }

    private StoreHolidayResponse toHolidayResponse(StoreHoliday holiday) {
        return StoreHolidayResponse.builder()
                .id(holiday.getId())
                .storeId(holiday.getStoreId())
                .date(holiday.getDate())
                .reason(holiday.getReason())
                .createdAt(holiday.getCreatedAt())
                .build();
    }
}