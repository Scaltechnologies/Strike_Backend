package com.admin_service.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AdminRefreshRequest {
    @NotBlank
    private String refreshToken;
}