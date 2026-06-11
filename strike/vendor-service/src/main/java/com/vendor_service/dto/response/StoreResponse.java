package com.vendor_service.dto.response;

import com.vendor_service.common.enums.StoreStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreResponse {

    private Long id;
    private Long vendorId;
    private String name;
    private String address;
    private String phone;
    private String email;
    private String category;
    private String description;
    private String logoUrl;
    private Double latitude;
    private Double longitude;
    private Double distanceKm;
    private StoreStatus status;
    private List<StoreTimingResponse> timings;
    private List<StoreHolidayResponse> holidays;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}