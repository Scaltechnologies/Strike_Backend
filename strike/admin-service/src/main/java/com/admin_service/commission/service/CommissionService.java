package com.admin_service.commission.service;

import com.admin_service.commission.dto.CommissionRecordResponse;
import com.admin_service.commission.dto.RecordCommissionRequest;
import com.admin_service.common.response.PageResponse;

import java.util.Map;

public interface CommissionService {
    CommissionRecordResponse record(RecordCommissionRequest request);
    PageResponse<CommissionRecordResponse> getAll(int page, int size);
    PageResponse<CommissionRecordResponse> getByVendor(Long vendorId, int page, int size);
    PageResponse<CommissionRecordResponse> getPending(int page, int size);
    CommissionRecordResponse settle(Long id);
    Map<String, Object> settleByVendor(Long vendorId);
    Map<String, Object> getStats();
}