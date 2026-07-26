package com.superhumans.exception;

import java.util.UUID;

public class DuplicateHourlyRecordException extends BusinessException {

    public DuplicateHourlyRecordException(UUID clinicalDayId, int recordHour) {
        super(ErrorCode.DUPLICATE_HOURLY_RECORD,
                "Hourly record already exists for hour " + recordHour + " on clinical day " + clinicalDayId);
    }
}