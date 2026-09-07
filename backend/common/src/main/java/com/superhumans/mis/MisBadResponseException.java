package com.superhumans.mis;

/**
 * MIS API answered with an HTTP error status. Carries the method name and the
 * numeric status only — never response bodies, credentials, or tokens.
 */
public class MisBadResponseException extends MisApiException {

    private final int statusCode;

    public MisBadResponseException(String methodName, int statusCode) {
        super("MIS API call failed: " + methodName + ", httpStatus=" + statusCode);
        this.statusCode = statusCode;
    }

    public MisBadResponseException(String methodName, int statusCode, Throwable cause) {
        super("MIS API call failed: " + methodName + ", httpStatus=" + statusCode, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
