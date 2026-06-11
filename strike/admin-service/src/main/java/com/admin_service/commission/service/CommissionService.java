package com.admin_service.commission.service;

import com.admin_service.commission.dto.CommissionRecordResponse;
import com.admin_service.commission.dto.RecordCommissionRequest;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

public interface CommissionService {
    CommissionRecordResponse record(RecordCommissionRequest request);
    List<CommissionRecordResponse> getAll();
    List<CommissionRecordResponse> getByVendor(Long vendorId);
    List<CommissionRecordResponse> getPending();
    CommissionRecordResponse settle(Long id);
    Map<String, Object> settleByVendor(Long vendorId);
    Map<String, Object> getStats();
}