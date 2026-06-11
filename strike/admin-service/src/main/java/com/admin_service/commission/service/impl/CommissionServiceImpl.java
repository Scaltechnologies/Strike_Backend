package com.admin_service.commission.service.impl;

import com.admin_service.commission.dto.CommissionRecordResponse;
import com.admin_service.commission.dto.RecordCommissionRequest;
import com.admin_service.commission.entity.CommissionRecord;
import com.admin_service.commission.repository.CommissionRecordRepository;
import com.admin_service.commission.service.CommissionService;
import com.admin_service.common.response.PageResponse;
import com.admin_service.vendor.entity.VendorRecord;
import com.admin_service.vendor.repository.VendorRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class CommissionServiceImpl implements CommissionService {

    private final CommissionRecordRepository commissionRepository;
    private final VendorRecordRepository vendorRecordRepository;

    private static final BigDecimal DEFAULT_RATE = new BigDecimal("10.00");

    @Override
    @Transactional
    public CommissionRecordResponse record(RecordCommissionRequest request) {
        BigDecimal rate = vendorRecordRepository.findById(request.getVendorId())
                .map(v -> v.getCommissionRate() != null ? v.getCommissionRate() : DEFAULT_RATE)
                .orElse(DEFAULT_RATE);

        BigDecimal commissionAmount = request.getSubscriptionAmount()
                .multiply(rate)
                .divide(new BigDecimal("100"), 2, RoundingMode.HALF_UP);

        CommissionRecord record = CommissionRecord.builder()
                .vendorId(request.getVendorId())
                .storeId(request.getStoreId())
                .subscriptionId(request.getSubscriptionId())
                .userId(request.getUserId())
                .subscriptionAmount(request.getSubscriptionAmount())
                .commissionRate(rate)
                .commissionAmount(commissionAmount)
                .status("PENDING")
                .build();

        return toResponse(commissionRepository.save(record));
    }

    @Override
    public PageResponse<CommissionRecordResponse> getAll(int page, int size) {
        return PageResponse.from(
                commissionRepository.findAll(PageRequest.of(page, size))
                        .map(this::toResponse));
    }

    @Override
    public PageResponse<CommissionRecordResponse> getByVendor(Long vendorId, int page, int size) {
        return PageResponse.from(
                commissionRepository.findByVendorId(vendorId, PageRequest.of(page, size))
                        .map(this::toResponse));
    }

    @Override
    public PageResponse<CommissionRecordResponse> getPending(int page, int size) {
        return PageResponse.from(
                commissionRepository.findByStatus("PENDING", PageRequest.of(page, size))
                        .map(this::toResponse));
    }

    @Override
    @Transactional
    public CommissionRecordResponse settle(Long id) {
        CommissionRecord record = commissionRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Commission record not found: " + id));
        if ("SETTLED".equals(record.getStatus())) {
            throw new RuntimeException("Commission already settled");
        }
        record.setStatus("SETTLED");
        record.setSettledAt(LocalDateTime.now());
        return toResponse(commissionRepository.save(record));
    }

    @Override
    @Transactional
    public Map<String, Object> settleByVendor(Long vendorId) {
        List<CommissionRecord> pending = commissionRepository.findByVendorIdAndStatus(vendorId, "PENDING");
        LocalDateTime now = LocalDateTime.now();
        pending.forEach(r -> {
            r.setStatus("SETTLED");
            r.setSettledAt(now);
        });
        commissionRepository.saveAll(pending);

        BigDecimal settledAmount = pending.stream()
                .map(CommissionRecord::getCommissionAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("vendorId", vendorId);
        result.put("settledCount", pending.size());
        result.put("settledAmount", settledAmount);
        result.put("settledAt", now.toString());
        return result;
    }

    @Override
    public Map<String, Object> getStats() {
        BigDecimal totalCommission = commissionRepository.sumCommissionByStatus("PENDING")
                .add(commissionRepository.sumCommissionByStatus("SETTLED"));
        BigDecimal pendingCommission = commissionRepository.sumCommissionByStatus("PENDING");
        BigDecimal settledCommission = commissionRepository.sumCommissionByStatus("SETTLED");
        BigDecimal totalRevenue = commissionRepository.totalSubscriptionRevenue();
        long totalRecords = commissionRepository.count();
        long pendingCount = commissionRepository.countByStatus("PENDING");

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("totalCommission", totalCommission);
        stats.put("pendingCommission", pendingCommission);
        stats.put("settledCommission", settledCommission);
        stats.put("totalSubscriptionRevenue", totalRevenue);
        stats.put("totalRecords", totalRecords);
        stats.put("pendingRecords", pendingCount);
        return stats;
    }

    private CommissionRecordResponse toResponse(CommissionRecord c) {
        String vendorName = vendorRecordRepository.findById(c.getVendorId())
                .map(VendorRecord::getHotelName).orElse("Unknown");
        return CommissionRecordResponse.builder()
                .id(c.getId())
                .vendorId(c.getVendorId())
                .vendorName(vendorName)
                .storeId(c.getStoreId())
                .subscriptionId(c.getSubscriptionId())
                .userId(c.getUserId())
                .subscriptionAmount(c.getSubscriptionAmount())
                .commissionRate(c.getCommissionRate())
                .commissionAmount(c.getCommissionAmount())
                .status(c.getStatus())
                .settledAt(c.getSettledAt())
                .createdAt(c.getCreatedAt())
                .build();
    }
}
