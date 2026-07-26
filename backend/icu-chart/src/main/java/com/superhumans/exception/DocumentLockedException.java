package com.superhumans.exception;

public class DocumentLockedException extends BusinessException {
    public DocumentLockedException(String message) {
        super(ErrorCode.DOCUMENT_LOCKED, message);
    }
}
