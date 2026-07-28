package com.superhumans.exception;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class NotFoundException extends BusinessException {    public NotFoundException(String message) {        super(ErrorCode.NOT_FOUND, message);    }}