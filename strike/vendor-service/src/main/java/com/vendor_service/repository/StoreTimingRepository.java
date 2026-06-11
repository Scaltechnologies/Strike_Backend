package com.vendor_service.repository;

import com.vendor_service.entity.StoreTiming;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Optional;

public interface StoreTimingRepository extends JpaRepository<StoreTiming, Long> {

    List<StoreTiming> findByStoreId(Long storeId);

    Optional<StoreTiming> findByStoreIdAndDayOfWeek(Long storeId, DayOfWeek dayOfWeek);

    void deleteByStoreId(Long storeId);
}