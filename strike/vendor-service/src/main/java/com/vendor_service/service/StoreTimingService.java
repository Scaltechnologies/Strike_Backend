package com.vendor_service.service;

import com.vendor_service.dto.request.StoreHolidayRequest;
import com.vendor_service.dto.request.StoreTimingRequest;
import com.vendor_service.dto.response.StoreHolidayResponse;
import com.vendor_service.dto.response.StoreTimingResponse;

import java.util.List;

public interface StoreTimingService {

    StoreTimingResponse addOrUpdateTiming(Long storeId, StoreTimingRequest request);

    List<StoreTimingResponse> getTimingsByStoreId(Long storeId);

    void deleteTiming(Long storeId, Long timingId);

    StoreHolidayResponse addHoliday(Long storeId, StoreHolidayRequest request);

    List<StoreHolidayResponse> getHolidaysByStoreId(Long storeId);

    void deleteHoliday(Long storeId, Long holidayId);
}