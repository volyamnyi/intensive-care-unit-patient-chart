package com.superhumans.exception;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VersionConflictException extends BusinessException {
    public VersionConflictException(String message) {
        super(ErrorCode.VERSION_CONFLICT, message);
    }
}
