package com.redemption_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

@SpringBootApplication(exclude = {UserDetailsServiceAutoConfiguration.class})
public class RedemptionServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(RedemptionServiceApplication.class, args);
    }
}