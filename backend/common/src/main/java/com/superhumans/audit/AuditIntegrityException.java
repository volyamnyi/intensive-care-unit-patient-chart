package com.superhumans.audit;

/** Signals an immutable audit ID or payload hash conflict that must never be overwritten. */
public class AuditIntegrityException extends RuntimeException {

    public AuditIntegrityException(String message) {
        super(message);
    }
}
