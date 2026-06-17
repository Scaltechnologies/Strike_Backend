package com.redemption_service.common.enums;

public enum RedemptionStatus {
    PENDING,     // user submitted, awaiting vendor approval — no balance deducted yet
    COMPLETED,
    REJECTED,    // vendor rejected — no balance deducted
    FAILED,
    REVERSED
}