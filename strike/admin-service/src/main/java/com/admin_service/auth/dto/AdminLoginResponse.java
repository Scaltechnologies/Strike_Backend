package com.admin_service.auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AdminLoginResponse {
    private Long adminId;
    private String email;
    private String name;
    private String role;
    private String token;
}