package com.superhumans.config;

import com.superhumans.exception.AppException;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

@ControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(value = { AppException.class })
    @ResponseBody
    public ResponseEntity<Error> handleException(AppException ex) {
        return ResponseEntity
                .status(ex.getStatus())
                .body(new Error(ex.getMessage()));
    }
}