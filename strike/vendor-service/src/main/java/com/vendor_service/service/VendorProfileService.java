
package com.vendor_service.service;

import com.vendor_service.dto.request.UpdateVendorProfileRequest;
import com.vendor_service.dto.response.VendorProfileResponse;
import com.vendor_service.entity.Vendor;
import com.vendor_service.entity.VendorProfile;
import com.vendor_service.repository.VendorProfileRepository;
import com.vendor_service.repository.VendorRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class VendorProfileService {

    private final VendorProfileRepository vendorProfileRepository;
    private final VendorRepository vendorRepository;

    public VendorProfileResponse getProfile(UUID vendorId) {

        VendorProfile profile = vendorProfileRepository
                .findById(vendorId)
                .orElseGet(() -> createProfile(vendorId));

        return mapToResponse(profile);
    }

    private VendorProfile createProfile(UUID vendorId) {

        Vendor vendor = vendorRepository.findById(vendorId)
                .orElseThrow(() -> new RuntimeException("Vendor not found"));

        VendorProfile profile = VendorProfile.builder()
                .vendorId(vendor.getId())
                .hotelName(vendor.getHotelName())
                .address(vendor.getAddress())
                .email(vendor.getEmail())
                .latitude(vendor.getLatitude())
                .longitude(vendor.getLongitude())
                .createdAt(LocalDateTime.now())
                .build();

        return vendorProfileRepository.save(profile);
    }

    public VendorProfileResponse updateProfile(UUID vendorId,
                                               UpdateVendorProfileRequest request) {

        VendorProfile profile = vendorProfileRepository.findById(vendorId)
                .orElseThrow(() -> new RuntimeException("Profile not found"));

        profile.setHotelName(request.getHotelName());
        profile.setAddress(request.getAddress());
        profile.setEmail(request.getEmail());
        profile.setLatitude(request.getLatitude());
        profile.setLongitude(request.getLongitude());
        profile.setDescription(request.getDescription());
        profile.setCuisineType(request.getCuisineType());
        profile.setUpdatedAt(LocalDateTime.now());

        vendorProfileRepository.save(profile);

        return mapToResponse(profile);
    }

    private VendorProfileResponse mapToResponse(VendorProfile profile) {

        return VendorProfileResponse.builder()
                .vendorId(profile.getVendorId())
                .hotelName(profile.getHotelName())
                .address(profile.getAddress())
                .email(profile.getEmail())
                .latitude(profile.getLatitude())
                .longitude(profile.getLongitude())
                .description(profile.getDescription())
                .cuisineType(profile.getCuisineType())
                .profileImage(profile.getProfileImage())
                .createdAt(profile.getCreatedAt())
                .build();
    }
}

