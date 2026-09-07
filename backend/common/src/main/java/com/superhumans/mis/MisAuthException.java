package com.superhumans.mis;

/**
 * MIS authentication failure: unreachable token endpoint, rejected credentials,
 * or a token response without a usable token. Never carries secrets in the message.
 */
public class MisAuthException extends MisApiException {

    public MisAuthException(String message) {
        super(message);
    }

    public MisAuthException(String message, Throwable cause) {
        super(message, cause);
    }
}
