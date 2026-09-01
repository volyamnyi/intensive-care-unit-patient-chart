package com.superhumans.prosthesismanufacturing.entity;

public enum FlowInstanceStatus {
    NEW,
    IN_PROGRESS,
    PAUSED,
    BLOCKED_PATIENT,
    BLOCKED_MATERIAL,
    WAITING_REVIEW,
    CORRECTION,
    FAILED_QC,
    COMPLETED,
    FAILED,
    BRANCHED
}
