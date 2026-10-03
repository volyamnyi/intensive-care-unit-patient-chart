package com.superhumans.exception;

import com.superhumans.audit.AuditActionDefinition.ActionType;
import com.superhumans.audit.AuditActionDefinition.EventClass;
import com.superhumans.audit.AuditActorResolver;
import com.superhumans.audit.AuditEvent;
import com.superhumans.audit.AuditEventRecorder;
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
@FieldDefaults(level = AccessLevel.PRIVATE)
public class GlobalExceptionHandler {

    AuditEventRecorder auditEventRecorder;

    /**
     * Optional injection: controller-slice tests import this advice without
     * the audit infrastructure, and denial auditing is best-effort anyway
     * (the recorder itself is fail-safe). Production always wires the bean.
     */
    @org.springframework.beans.factory.annotation.Autowired(required = false)
    void setAuditEventRecorder(AuditEventRecorder auditEventRecorder) {
        this.auditEventRecorder = auditEventRecorder;
    }

    private static final java.util.regex.Pattern ID_SEGMENT = java.util.regex.Pattern.compile(
            "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}|\\d+");

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(
                new ErrorResponse(ex.getCode(), ex.getMessage(), UUID.randomUUID().toString()));
    }

    @ExceptionHandler(DocumentLockedException.class)
    public ResponseEntity<ErrorResponse> handleDocumentLocked(DocumentLockedException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(
                new ErrorResponse(ex.getCode(), ex.getMessage(), UUID.randomUUID().toString()));
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ErrorResponse> handleSecurity(SecurityException ex) {
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                new ErrorResponse(ErrorCode.FORBIDDEN, ex.getMessage(), UUID.randomUUID().toString()));
    }

    @ExceptionHandler(org.springframework.security.authorization.AuthorizationDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAuthorizationDenied(
            org.springframework.security.authorization.AuthorizationDeniedException ex,
            jakarta.servlet.http.HttpServletRequest request) {
        // ActorPolicy.USER catalog policy: anonymous denials carry no user
        // actor, and building the event would fail validation — the 403
        // itself must never break because of audit.
        if (auditEventRecorder != null
                && AuditActorResolver.fromCurrentContext().type() == AuditEvent.ActorType.USER) {
            auditEventRecorder.record(AuditEvent.builder()
                    .actor(AuditActorResolver.fromCurrentContext())
                    .eventClass(EventClass.SECURITY)
                    .module("platform")
                    .functionalArea("authz")
                    .action("platform.auth.access.denied")
                    .actionType(ActionType.ACCESS_DENIED)
                    .outcome(AuditEvent.AuditOutcome.DENIED)
                    .source(AuditEvent.AuditSource.API)
                    .httpContext(new AuditEvent.AuditHttpContext(
                            request.getMethod(), sanitizeRoute(request.getServletPath())))
                    .build());
        }
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(
                new ErrorResponse(ErrorCode.FORBIDDEN, "Access denied", UUID.randomUUID().toString()));
    }

    /**
     * Redacts identifier path segments (UUIDs and digit runs) so the denied
     * route template carries no PII.
     */
    static String sanitizeRoute(String servletPath) {
        if (servletPath == null || servletPath.isBlank()) {
            return null;
        }
        String redacted = ID_SEGMENT.matcher(servletPath).replaceAll("{id}");
        return redacted.length() > 200 ? redacted.substring(0, 200) : redacted;
    }

    @ExceptionHandler(VersionConflictException.class)
    public ResponseEntity<ErrorResponse> handleVersionConflict(VersionConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ErrorResponse(ex.getCode(), ex.getMessage(), UUID.randomUUID().toString()));
    }

    @ExceptionHandler(OptimisticLockException.class)
    public ResponseEntity<ErrorResponse> handleOptimisticLock(OptimisticLockException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT).body(
                new ErrorResponse(ErrorCode.VERSION_CONFLICT, "Concurrent modification detected.", UUID.randomUUID().toString()));
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_CONTENT).body(
                new ErrorResponse(ex.getCode(), ex.getMessage(), UUID.randomUUID().toString()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(
                new ErrorResponse(ErrorCode.VALIDATION_ERROR, message, UUID.randomUUID().toString()));
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(BadRequestException ex) {
        return ResponseEntity.badRequest().body(
                new ErrorResponse(ErrorCode.BAD_REQUEST, ex.getMessage(), UUID.randomUUID().toString()));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        String message = ex.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                .collect(Collectors.joining("; "));
        return ResponseEntity.badRequest().body(
                new ErrorResponse(ErrorCode.VALIDATION_ERROR, message, UUID.randomUUID().toString()));
    }

    @ExceptionHandler(InvalidDataAccessApiUsageException.class)
    public ResponseEntity<ErrorResponse> handleInvalidDataAccessApiUsage(InvalidDataAccessApiUsageException ex) {
        if (ex.getCause() instanceof IllegalArgumentException iae) {
            return handleIllegalArgument(iae);
        }
        log.error("Unexpected data access error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ErrorResponse(ErrorCode.INTERNAL_ERROR, "An internal error occurred.", UUID.randomUUID().toString()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(
                new ErrorResponse(ErrorCode.VALIDATION_ERROR, ex.getMessage(), UUID.randomUUID().toString()));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String message = "Invalid value '" + ex.getValue() + "' for parameter '" + ex.getName() + "': " + ex.getMessage();
        return ResponseEntity.badRequest().body(
                new ErrorResponse(ErrorCode.BAD_REQUEST, message, UUID.randomUUID().toString()));
    }

    @ExceptionHandler(RuntimeException.class)
    public ResponseEntity<ErrorResponse> handleRuntime(RuntimeException ex) {
        log.error("Unexpected error", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                new ErrorResponse(ErrorCode.INTERNAL_ERROR, "An internal error occurred.", UUID.randomUUID().toString()));
    }
}
