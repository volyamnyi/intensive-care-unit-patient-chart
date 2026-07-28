package com.superhumans.exception;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;import java.util.UUID;@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DuplicateHourlyRecordException extends BusinessException {    public DuplicateHourlyRecordException(UUID clinicalDayId, int recordHour) {        super(ErrorCode.DUPLICATE_HOURLY_RECORD,                "Hourly record already exists for hour " + recordHour + " on clinical day " + clinicalDayId);    }}