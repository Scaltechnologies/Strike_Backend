package com.user_service.store.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
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
    private String status;
    private List<StoreTimingResponse> timings;
    private List<StoreHolidayResponse> holidays;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}