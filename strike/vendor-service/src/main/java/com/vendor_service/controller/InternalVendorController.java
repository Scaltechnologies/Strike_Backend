package com.vendor_service.controller;

import com.vendor_service.common.enums.StoreStatus;
import com.vendor_service.dto.request.InitVendorProfileRequest;
import com.vendor_service.dto.response.StoreResponse;
import com.vendor_service.entity.Store;
import com.vendor_service.repository.StoreRepository;
import com.vendor_service.service.StoreService;
import com.vendor_service.service.VendorProfileService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/internal/vendors")
@RequiredArgsConstructor
public class InternalVendorController {

    private final VendorProfileService vendorProfileService;
    private final StoreRepository storeRepository;
    private final StoreService storeService;

    /**
     * Called by auth-service after a vendor completes registration.
     * Creates vendor profile + initial store record if they don't exist yet (idempotent).
     */
    @PostMapping("/{vendorId}/init")
    public ResponseEntity<?> initVendorProfile(
            @PathVariable Long vendorId,
            @Valid @RequestBody InitVendorProfileRequest request) {

        request.setVendorId(vendorId);
        vendorProfileService.initProfile(request);

        boolean storeExists = !storeRepository.findByVendorId(vendorId).isEmpty();
        if (!storeExists) {
            Store store = Store.builder()
                    .vendorId(vendorId)
                    .name(request.getShopName())
                    .address(request.getAddress() != null ? request.getAddress() : "")
                    .phone(request.getMobile())
                    .email(request.getEmail())
                    .status(StoreStatus.ACTIVE)
                    .build();
            storeRepository.save(store);
        }

        return ResponseEntity.ok("Vendor profile initialized");
    }

    @GetMapping("/{vendorId}/store")
    public ResponseEntity<StoreResponse> getVendorStore(@PathVariable Long vendorId) {
        try {
            return ResponseEntity.ok(storeService.getStoreByVendorId(vendorId));
        } catch (Exception e) {
            return ResponseEntity.notFound().build();
        }
    }
}