package com.vendor_service.controller;

import com.vendor_service.common.annotation.CurrentVendorId;
import com.vendor_service.common.constants.ApiRoutes;
import com.vendor_service.common.response.ApiResponse;
import com.vendor_service.dto.request.StoreHolidayRequest;
import com.vendor_service.dto.request.StoreTimingRequest;
import com.vendor_service.dto.response.StoreHolidayResponse;
import com.vendor_service.dto.response.StoreTimingResponse;
import com.vendor_service.service.StoreService;
import com.vendor_service.service.StoreTimingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class StoreTimingController {

    private final StoreTimingService storeTimingService;
    private final StoreService storeService;

    // ── Timings ──────────────────────────────────────────────────────────────

    @PostMapping(ApiRoutes.Store.TIMINGS)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('VENDOR')")
    public ApiResponse<StoreTimingResponse> addOrUpdateTiming(
            @CurrentVendorId Long vendorId,
            @PathVariable Long id,
            @Valid @RequestBody StoreTimingRequest request
    ) {
        storeService.validateStoreOwnership(id, vendorId);
        return ApiResponse.success("Timing saved successfully", storeTimingService.addOrUpdateTiming(id, request));
    }

    @GetMapping(ApiRoutes.Store.TIMINGS)
    public ApiResponse<List<StoreTimingResponse>> getTimings(
            @PathVariable Long id
    ) {
        return ApiResponse.success(storeTimingService.getTimingsByStoreId(id));
    }

    @DeleteMapping(ApiRoutes.Store.TIMINGS + "/{timingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('VENDOR')")
    public void deleteTiming(
            @CurrentVendorId Long vendorId,
            @PathVariable Long id,
            @PathVariable Long timingId
    ) {
        storeService.validateStoreOwnership(id, vendorId);
        storeTimingService.deleteTiming(id, timingId);
    }

    // ── Holidays ─────────────────────────────────────────────────────────────

    @PostMapping(ApiRoutes.Store.HOLIDAYS)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('VENDOR')")
    public ApiResponse<StoreHolidayResponse> addHoliday(
            @CurrentVendorId Long vendorId,
            @PathVariable Long id,
            @Valid @RequestBody StoreHolidayRequest request
    ) {
        storeService.validateStoreOwnership(id, vendorId);
        return ApiResponse.success("Holiday added successfully", storeTimingService.addHoliday(id, request));
    }

    @GetMapping(ApiRoutes.Store.HOLIDAYS)
    public ApiResponse<List<StoreHolidayResponse>> getHolidays(
            @PathVariable Long id
    ) {
        return ApiResponse.success(storeTimingService.getHolidaysByStoreId(id));
    }

    @DeleteMapping(ApiRoutes.Store.HOLIDAYS + "/{holidayId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('VENDOR')")
    public void deleteHoliday(
            @CurrentVendorId Long vendorId,
            @PathVariable Long id,
            @PathVariable Long holidayId
    ) {
        storeService.validateStoreOwnership(id, vendorId);
        storeTimingService.deleteHoliday(id, holidayId);
    }
}