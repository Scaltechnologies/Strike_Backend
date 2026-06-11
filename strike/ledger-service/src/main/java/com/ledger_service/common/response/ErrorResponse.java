package com.ledger_service.common.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorResponse {
    @Builder.Default private boolean success = false;
    private int status;
    private String error;
    private String message;
    private Map<String, String> errors;
    @Builder.Default private LocalDateTime timestamp = LocalDateTime.now();
}