package com.vendor_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StoreHolidayResponse {

    private Long id;
    private Long storeId;
    private LocalDate date;
    private String reason;
    private LocalDateTime createdAt;
}