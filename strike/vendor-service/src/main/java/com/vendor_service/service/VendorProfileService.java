package com.vendor_service.service;

import com.vendor_service.dto.request.UpdateVendorProfileRequest;
import com.vendor_service.dto.response.VendorProfileResponse;
import com.vendor_service.entity.VendorProfile;
import com.vendor_service.repository.VendorProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class VendorProfileService {

    private final VendorProfileRepository vendorProfileRepository;

    public VendorProfileResponse updateProfile(
            Long vendorId,
            UpdateVendorProfileRequest request
    ) {

        VendorProfile profile = vendorProfileRepository
                .findByVendorId(vendorId)
                .orElse(
                        VendorProfile.builder()
                                .vendorId(vendorId)
                                .build()
                );

        profile.setShopName(request.getShopName());
        profile.setOwnerName(request.getOwnerName());
        profile.setMobile(request.getMobile());
        profile.setAddress(request.getAddress());
        profile.setCategory(request.getCategory());
        profile.setDescription(request.getDescription());
        profile.setLogoUrl(request.getLogoUrl());

        VendorProfile saved =
                vendorProfileRepository.save(profile);

        return mapToResponse(saved);
    }

    public VendorProfileResponse getProfile(Long vendorId) {

        VendorProfile profile = vendorProfileRepository
                .findByVendorId(vendorId)
                .orElseThrow(() ->
                        new RuntimeException("Vendor profile not found")
                );

        return mapToResponse(profile);
    }

    private VendorProfileResponse mapToResponse(
            VendorProfile profile
    ) {

        return VendorProfileResponse.builder()
                .id(profile.getId())
                .vendorId(profile.getVendorId())
                .shopName(profile.getShopName())
                .ownerName(profile.getOwnerName())
                .mobile(profile.getMobile())
                .address(profile.getAddress())
                .category(profile.getCategory())
                .description(profile.getDescription())
                .logoUrl(profile.getLogoUrl())
                .build();
    }
}