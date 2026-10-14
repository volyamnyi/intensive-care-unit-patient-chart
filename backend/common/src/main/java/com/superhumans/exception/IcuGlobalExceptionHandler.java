package com.superhumans.exception;

import com.superhumans.dto.ErrorResponse;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.UUID;

/** Feature advice: ordered BEFORE the platform advice so its specific handlers win. */
@Slf4j
@Order(1)
@ControllerAdvice
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class IcuGlobalExceptionHandler {

    @ExceptionHandler(ClinicalDayAlreadyOpenException.class)
    public ResponseEntity<ErrorResponse> handleClinicalDayAlreadyOpen(ClinicalDayAlreadyOpenException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(
                new ErrorResponse(ex.getCode(), ex.getMessage(), UUID.randomUUID().toString()));
    }

    @ExceptionHandler(DuplicateHourlyRecordException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateHourlyRecord(DuplicateHourlyRecordException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ErrorResponse(ex.getCode(), ex.getMessage(), UUID.randomUUID().toString()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        String message = ex.getMostSpecificCause().getMessage();
        if (message != null && message.contains("uk_hourly_records_clinical_day_record_hour")) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    new ErrorResponse(ErrorCode.DUPLICATE_HOURLY_RECORD, "Hourly record already exists for this hour", UUID.randomUUID().toString()));
        }
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ErrorResponse(ErrorCode.VERSION_CONFLICT, "Data integrity violation", UUID.randomUUID().toString()));
    }
}