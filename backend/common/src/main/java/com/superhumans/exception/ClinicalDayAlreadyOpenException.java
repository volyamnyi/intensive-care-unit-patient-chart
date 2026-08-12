package com.superhumans.exception;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ClinicalDayAlreadyOpenException extends BusinessException {    public ClinicalDayAlreadyOpenException(String message) {        super(ErrorCode.CLINICAL_DAY_ALREADY_OPEN, message);    }}