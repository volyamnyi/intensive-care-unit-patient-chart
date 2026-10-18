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

    /**
     * Matches the X-Correlation-Id response header (RequestCorrelationFilter);
     * falls back to a fresh UUID for non-request callers (#403).
     */
    static String correlationId() {
        com.superhumans.audit.AuditRequestContext.Context context =
                com.superhumans.audit.AuditRequestContext.current();
        return context == null ? java.util.UUID.randomUUID().toString()
                : context.correlationId().toString();
    }

    @ExceptionHandler(ClinicalDayAlreadyOpenException.class)
    public ResponseEntity<ErrorResponse> handleClinicalDayAlreadyOpen(ClinicalDayAlreadyOpenException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(
                new ErrorResponse(ex.getCode(), ex.getMessage(), correlationId()));
    }

    @ExceptionHandler(DuplicateHourlyRecordException.class)
    public ResponseEntity<ErrorResponse> handleDuplicateHourlyRecord(DuplicateHourlyRecordException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ErrorResponse(ex.getCode(), ex.getMessage(), correlationId()));
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrity(DataIntegrityViolationException ex) {
        String message = ex.getMostSpecificCause().getMessage();
        if (message != null && message.contains("uk_hourly_records_clinical_day_record_hour")) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    new ErrorResponse(ErrorCode.DUPLICATE_HOURLY_RECORD, "Hourly record already exists for this hour", correlationId()));
        }
        // #382: the race loser of a concurrent create hits the backing unique
        // index. The response must match the sequential duplicate's contract, not
        // a generic 409 - a user who clicked twice fast gets the same answer as a
        // user who re-created what already exists.
        if (message != null && message.contains("uq_episodes_active_patient")) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(
                    new ErrorResponse(ErrorCode.EPISODE_ALREADY_ACTIVE,
                            "Active episode already exists for this patient", correlationId()));
        }
        if (message != null && message.contains("uq_clinical_days_episode_day")) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    new ErrorResponse(ErrorCode.VERSION_CONFLICT,
                            "Concurrent clinical day creation detected - reload the episode", correlationId()));
        }
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ErrorResponse(ErrorCode.VERSION_CONFLICT, "Data integrity violation", correlationId()));
    }
}
