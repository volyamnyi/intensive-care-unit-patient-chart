package com.superhumans.mis;

/**
 * Base class for all MIS integration failures.
 * <p>
 * Messages never carry credentials or tokens — safe for logs and API error responses.
 */
public class MisApiException extends RuntimeException {

    public MisApiException(String message) {
        super(message);
    }

    public MisApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
