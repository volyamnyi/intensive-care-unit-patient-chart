package com.superhumans.exception;

import com.superhumans.dto.ErrorResponse;
import jakarta.persistence.OptimisticLockException;
import jakarta.validation.ConstraintViolationException;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.InvalidDataAccessApiUsageException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@ControllerAdvice
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class GlobalExceptionHandler {

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
        if (message != null && message.contains("ukq1fhtacn518q9viq4oonaghi9")) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(
                    new ErrorResponse(ErrorCode.DUPLICATE_HOURLY_RECORD, "Hourly record already exists for this hour", UUID.randomUUID().toString()));
        }
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ErrorResponse(ErrorCode.VERSION_CONFLICT, "Data integrity violation", UUID.randomUUID().toString()));
    }
}