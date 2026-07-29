package com.superhumans.exception;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public final class ErrorCode {
    ErrorCode() {}

    public static String EPISODE_ALREADY_ACTIVE = "EPISODE_ALREADY_ACTIVE";
    public static String CLINICAL_DAY_ALREADY_OPEN = "CLINICAL_DAY_ALREADY_OPEN";
    public static String INVALID_CLINICAL_VALUE = "INVALID_CLINICAL_VALUE";
    public static String ORDER_ALREADY_COMPLETED = "ORDER_ALREADY_COMPLETED";
    public static String SIGNATURE_REQUIRED = "SIGNATURE_REQUIRED";
    public static String DOCUMENT_LOCKED = "DOCUMENT_LOCKED";
    public static String VERSION_CONFLICT = "VERSION_CONFLICT";
    public static String BAD_REQUEST = "BAD_REQUEST";
    public static String NOT_FOUND = "NOT_FOUND";
    public static String INTERNAL_ERROR = "INTERNAL_ERROR";
    public static String VALIDATION_ERROR = "VALIDATION_ERROR";
    public static String DUPLICATE_HOURLY_RECORD = "DUPLICATE_HOURLY_RECORD";
    public static String BUSINESS_RULE = "BUSINESS_RULE";
    public static String PAST_HOUR_ORDER = "PAST_HOUR_ORDER";
}
