package com.superhumans.mis;

/**
 * MIS API call exceeded the configured read/connect timeout.
 */
public class MisTimeoutException extends MisApiException {

    public MisTimeoutException(String message) {
        super(message);
    }

    public MisTimeoutException(String message, Throwable cause) {
        super(message, cause);
    }
}
