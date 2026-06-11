package com.vendor_service.service.impl;

import com.vendor_service.common.exception.BadRequestException;
import com.vendor_service.common.exception.ResourceNotFoundException;
import com.vendor_service.dto.request.StoreHolidayRequest;
import com.vendor_service.dto.request.StoreTimingRequest;
import com.vendor_service.dto.response.StoreHolidayResponse;
import com.vendor_service.dto.response.StoreTimingResponse;
import com.vendor_service.entity.StoreHoliday;
import com.vendor_service.entity.StoreTiming;
import com.vendor_service.repository.StoreHolidayRepository;
import com.vendor_service.repository.StoreRepository;
import com.vendor_service.repository.StoreTimingRepository;
import com.vendor_service.service.StoreTimingService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class StoreTimingServiceImpl implements StoreTimingService {

    private final StoreRepository storeRepository;
    private final StoreTimingRepository storeTimingRepository;
    private final StoreHolidayRepository storeHolidayRepository;

    @Override
    public StoreTimingResponse addOrUpdateTiming(Long storeId, StoreTimingRequest request) {
        validateStoreExists(storeId);

        if (!Boolean.TRUE.equals(request.getIsClosed())
                && (request.getOpenTime() == null || request.getCloseTime() == null)) {
            throw new BadRequestException("Open time and close time are required when store is not closed");
        }

        StoreTiming timing = storeTimingRepository
                .findByStoreIdAndDayOfWeek(storeId, request.getDayOfWeek())
                .orElse(StoreTiming.builder()
                        .storeId(storeId)
                        .dayOfWeek(request.getDayOfWeek())
                        .build());

        timing.setOpenTime(request.getOpenTime());
        timing.setCloseTime(request.getCloseTime());
        timing.setIsClosed(Boolean.TRUE.equals(request.getIsClosed()));

        return toTimingResponse(storeTimingRepository.save(timing));
    }

    @Override
    public List<StoreTimingResponse> getTimingsByStoreId(Long storeId) {
        validateStoreExists(storeId);
        return storeTimingRepository.findByStoreId(storeId)
                .stream()
                .map(this::toTimingResponse)
                .toList();
    }

    @Override
    public void deleteTiming(Long storeId, Long timingId) {
        StoreTiming timing = storeTimingRepository.findById(timingId)
                .orElseThrow(() -> new ResourceNotFoundException("Timing not found with id: " + timingId));

        if (!timing.getStoreId().equals(storeId)) {
            throw new BadRequestException("Timing does not belong to store: " + storeId);
        }

        storeTimingRepository.deleteById(timingId);
    }

    @Override
    public StoreHolidayResponse addHoliday(Long storeId, StoreHolidayRequest request) {
        validateStoreExists(storeId);

        storeHolidayRepository.findByStoreIdAndDate(storeId, request.getDate())
                .ifPresent(h -> {
                    throw new BadRequestException("Holiday already exists for date: " + request.getDate());
                });

        StoreHoliday holiday = StoreHoliday.builder()
                .storeId(storeId)
                .date(request.getDate())
                .reason(request.getReason())
                .build();

        return toHolidayResponse(storeHolidayRepository.save(holiday));
    }

    @Override
    public List<StoreHolidayResponse> getHolidaysByStoreId(Long storeId) {
        validateStoreExists(storeId);
        return storeHolidayRepository.findByStoreId(storeId)
                .stream()
                .map(this::toHolidayResponse)
                .toList();
    }

    @Override
    public void deleteHoliday(Long storeId, Long holidayId) {
        StoreHoliday holiday = storeHolidayRepository.findById(holidayId)
                .orElseThrow(() -> new ResourceNotFoundException("Holiday not found with id: " + holidayId));

        if (!holiday.getStoreId().equals(storeId)) {
            throw new BadRequestException("Holiday does not belong to store: " + storeId);
        }

        storeHolidayRepository.deleteById(holidayId);
    }

    private void validateStoreExists(Long storeId) {
        if (!storeRepository.existsById(storeId)) {
            throw new ResourceNotFoundException("Store not found with id: " + storeId);
        }
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