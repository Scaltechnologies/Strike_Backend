package com.vendor_service.repository;

import com.vendor_service.common.enums.StoreStatus;
import com.vendor_service.entity.Store;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StoreRepository extends JpaRepository<Store, Long> {

    List<Store> findByVendorId(Long vendorId);

    List<Store> findAllByStatus(StoreStatus status);
}