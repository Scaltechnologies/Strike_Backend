package com.authservice.controller;

import com.authservice.common.response.PageResponse;
import com.authservice.user.dto.UserAuthResponse;
import com.authservice.user.entity.UserAuth;
import com.authservice.user.repository.UserAuthRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/users")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserAuthRepository userAuthRepository;

    @GetMapping
    public PageResponse<UserAuthResponse> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return PageResponse.from(
                userAuthRepository.findAll(PageRequest.of(page, size))
                        .map(this::toResponse));
    }

    @GetMapping("/count")
    public long countUsers() {
        return userAuthRepository.count();
    }

    @GetMapping("/{userId}")
    public ResponseEntity<UserAuthResponse> getUserById(@PathVariable Long userId) {
        return userAuthRepository.findById(userId)
                .map(u -> ResponseEntity.ok(toResponse(u)))
                .orElse(ResponseEntity.notFound().build());
    }

    @PatchMapping("/{userId}/ban")
    public ResponseEntity<Void> banUser(@PathVariable Long userId) {
        userAuthRepository.findById(userId).ifPresent(u -> {
            u.setBanned(true);
            userAuthRepository.save(u);
        });
        return ResponseEntity.ok().build();
    }

    @PatchMapping("/{userId}/unban")
    public ResponseEntity<Void> unbanUser(@PathVariable Long userId) {
        userAuthRepository.findById(userId).ifPresent(u -> {
            u.setBanned(false);
            userAuthRepository.save(u);
        });
        return ResponseEntity.ok().build();
    }

    private UserAuthResponse toResponse(UserAuth user) {
        return UserAuthResponse.builder()
                .id(user.getId())
                .mobileNumber(user.getMobileNumber())
                .verified(user.getVerified())
                .banned(user.getBanned())
                .createdAt(user.getCreatedAt())
                .updatedAt(user.getUpdatedAt())
                .build();
    }
}