package com.vendor_service.controller;

import com.vendor_service.common.annotation.CurrentVendorId;
import com.vendor_service.common.response.ApiResponse;
import com.vendor_service.dto.request.StoreHolidayRequest;
import com.vendor_service.dto.request.StoreTimingRequest;
import com.vendor_service.dto.response.StoreHolidayResponse;
import com.vendor_service.dto.response.StoreTimingResponse;
import com.vendor_service.service.StoreService;
import com.vendor_service.service.StoreTimingService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/vendor/stores")
@RequiredArgsConstructor
public class StoreTimingController {

    private final StoreTimingService storeTimingService;
    private final StoreService storeService;

    // ── Timings ──────────────────────────────────────────────────────────────

    @PostMapping("/{id}/timings")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('VENDOR')")
    public ApiResponse<StoreTimingResponse> addOrUpdateTiming(
            @CurrentVendorId Long vendorId,
            @PathVariable Long id,
            @Valid @RequestBody StoreTimingRequest request
    ) {
        log.info("[StoreTimingController] POST /{}/timings — vendorId={}, day={}, isClosed={}",
                id, vendorId, request.getDayOfWeek(), request.getIsClosed());
        storeService.validateStoreOwnership(id, vendorId);
        StoreTimingResponse result = storeTimingService.addOrUpdateTiming(id, request);
        log.info("[StoreTimingController] timing saved — timingId={}", result.getId());
        return ApiResponse.success("Timing saved successfully", result);
    }

    @GetMapping("/{id}/timings")
    public ApiResponse<List<StoreTimingResponse>> getTimings(
            @PathVariable Long id
    ) {
        log.info("[StoreTimingController] GET /{}/timings", id);
        return ApiResponse.success(storeTimingService.getTimingsByStoreId(id));
    }

    @DeleteMapping("/{id}/timings/{timingId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('VENDOR')")
    public void deleteTiming(
            @CurrentVendorId Long vendorId,
            @PathVariable Long id,
            @PathVariable Long timingId
    ) {
        log.info("[StoreTimingController] DELETE /{}/timings/{} — vendorId={}", id, timingId, vendorId);
        storeService.validateStoreOwnership(id, vendorId);
        storeTimingService.deleteTiming(id, timingId);
    }

    // ── Holidays ─────────────────────────────────────────────────────────────

    @PostMapping("/{id}/holidays")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('VENDOR')")
    public ApiResponse<StoreHolidayResponse> addHoliday(
            @CurrentVendorId Long vendorId,
            @PathVariable Long id,
            @Valid @RequestBody StoreHolidayRequest request
    ) {
        log.info("[StoreTimingController] POST /{}/holidays — vendorId={}, date={}", id, vendorId, request.getDate());
        storeService.validateStoreOwnership(id, vendorId);
        StoreHolidayResponse result = storeTimingService.addHoliday(id, request);
        log.info("[StoreTimingController] holiday added — holidayId={}", result.getId());
        return ApiResponse.success("Holiday added successfully", result);
    }

    @GetMapping("/{id}/holidays")
    public ApiResponse<List<StoreHolidayResponse>> getHolidays(
            @PathVariable Long id
    ) {
        log.info("[StoreTimingController] GET /{}/holidays", id);
        return ApiResponse.success(storeTimingService.getHolidaysByStoreId(id));
    }

    @DeleteMapping("/{id}/holidays/{holidayId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('VENDOR')")
    public void deleteHoliday(
            @CurrentVendorId Long vendorId,
            @PathVariable Long id,
            @PathVariable Long holidayId
    ) {
        log.info("[StoreTimingController] DELETE /{}/holidays/{} — vendorId={}", id, holidayId, vendorId);
        storeService.validateStoreOwnership(id, vendorId);
        storeTimingService.deleteHoliday(id, holidayId);
    }
}