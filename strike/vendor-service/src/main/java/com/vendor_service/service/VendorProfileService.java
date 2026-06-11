package com.vendor_service.service;

import com.vendor_service.common.exception.ResourceNotFoundException;
import com.vendor_service.dto.request.InitVendorProfileRequest;
import com.vendor_service.dto.request.UpdateVendorProfileRequest;
import com.vendor_service.dto.response.VendorProfileResponse;
import com.vendor_service.entity.VendorProfile;
import com.vendor_service.repository.VendorProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class VendorProfileService {

    private final VendorProfileRepository vendorProfileRepository;
    private final RestTemplate restTemplate;

    @Value("${services.admin-url}")
    private String adminServiceUrl;

    /**
     * Called by InternalVendorController when a vendor is first registered.
     * Creates the profile if it does not exist; idempotent on repeat calls.
     */
    public VendorProfileResponse initProfile(InitVendorProfileRequest request) {
        VendorProfile profile = vendorProfileRepository
                .findByVendorId(request.getVendorId())
                .orElseGet(() -> VendorProfile.builder()
                        .vendorId(request.getVendorId())
                        .shopName(request.getShopName())
                        .mobile(request.getMobile())
                        .address(request.getAddress())
                        .email(request.getEmail())
                        .build());
        return mapToResponse(vendorProfileRepository.save(profile));
    }

    /**
     * Null-safe profile update — only non-null fields are changed.
     * Works for both PUT (full replace) and PATCH (partial) semantics.
     */
    public VendorProfileResponse updateProfile(Long vendorId, UpdateVendorProfileRequest request) {
        VendorProfile profile = vendorProfileRepository
                .findByVendorId(vendorId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Profile not found for vendor: " + vendorId + ". Please complete initial setup."));

        if (request.getShopName()    != null) profile.setShopName(request.getShopName());
        if (request.getOwnerName()   != null) profile.setOwnerName(request.getOwnerName());
        if (request.getMobile()      != null) profile.setMobile(request.getMobile());
        if (request.getEmail()       != null) profile.setEmail(request.getEmail());
        if (request.getAddress()     != null) profile.setAddress(request.getAddress());
        if (request.getCategory()    != null) profile.setCategory(request.getCategory());
        if (request.getDescription() != null) profile.setDescription(request.getDescription());
        if (request.getLogoUrl()     != null) profile.setLogoUrl(request.getLogoUrl());

        return mapToResponse(vendorProfileRepository.save(profile));
    }

    public VendorProfileResponse getProfile(Long vendorId) {
        VendorProfile profile = vendorProfileRepository
                .findByVendorId(vendorId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Profile not found for vendor: " + vendorId));
        return mapToResponse(profile);
    }

    /**
     * Calls admin-service to retrieve this vendor's approval status.
     * Returns a map with: status, rejectionReason.
     */
    @SuppressWarnings("unchecked")
    public Map<String, Object> getVendorStatus(Long vendorId) {
        try {
            Object response = restTemplate.getForObject(
                    adminServiceUrl + "/internal/admin/vendors/" + vendorId + "/status",
                    Object.class);
            if (response instanceof Map<?, ?> m) {
                return (Map<String, Object>) m;
            }
        } catch (Exception e) {
            return Map.of(
                    "vendorId", vendorId,
                    "status", "UNKNOWN",
                    "message", "Could not retrieve status from admin service: " + e.getMessage());
        }
        return Map.of("vendorId", vendorId, "status", "UNKNOWN");
    }

    private VendorProfileResponse mapToResponse(VendorProfile profile) {
        return VendorProfileResponse.builder()
                .id(profile.getId())
                .vendorId(profile.getVendorId())
                .shopName(profile.getShopName())
                .ownerName(profile.getOwnerName())
                .mobile(profile.getMobile())
                .email(profile.getEmail())
                .address(profile.getAddress())
                .category(profile.getCategory())
                .description(profile.getDescription())
                .logoUrl(profile.getLogoUrl())
                .createdAt(profile.getCreatedAt())
                .updatedAt(profile.getUpdatedAt())
                .build();
    }
}