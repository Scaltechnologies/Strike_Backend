package com.authservice.user.dto;

import lombok.*;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthResponse {

    private Long userId;

    private String mobileNumber;

    private String token;

    private Boolean newUser;

    private String message;
}