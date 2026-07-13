package com.superhumans.exception;

public class ClinicalDayAlreadyOpenException extends BusinessException {
    public ClinicalDayAlreadyOpenException(String message) {
        super(ErrorCode.CLINICAL_DAY_ALREADY_OPEN, message);
    }
}
