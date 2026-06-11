package com.user_service.store.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class StoreHolidayResponse {
    private Long id;
    private Long storeId;
    private LocalDate date;
    private String reason;
    private LocalDateTime createdAt;
}