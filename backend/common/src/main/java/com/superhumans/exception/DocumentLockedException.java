package com.superhumans.exception;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DocumentLockedException extends BusinessException {
    public DocumentLockedException(String message) {
        super(ErrorCode.DOCUMENT_LOCKED, message);
    }
}
