package com.superhumans.exception;

public class VersionConflictException extends BusinessException {
    public VersionConflictException(String message) {
        super(ErrorCode.VERSION_CONFLICT, message);
    }
}
