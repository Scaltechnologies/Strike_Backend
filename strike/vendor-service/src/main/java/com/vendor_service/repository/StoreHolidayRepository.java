package com.vendor_service.repository;

import com.vendor_service.entity.StoreHoliday;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface StoreHolidayRepository extends JpaRepository<StoreHoliday, Long> {

    List<StoreHoliday> findByStoreId(Long storeId);

    Optional<StoreHoliday> findByStoreIdAndDate(Long storeId, LocalDate date);

    void deleteByStoreId(Long storeId);
}