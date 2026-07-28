package com.superhumans.exception;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import lombok.Getter;

@Getter
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BusinessException extends RuntimeException {
    String code;

    public BusinessException(String code, String message) {
        super(message);
        this.code = code;
    }
}
