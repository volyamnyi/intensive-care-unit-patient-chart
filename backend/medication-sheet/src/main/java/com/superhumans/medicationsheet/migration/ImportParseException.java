package com.superhumans.medicationsheet.migration;

/** Unchecked failure to parse a legacy CSV/JSON payload. */
public class ImportParseException extends RuntimeException {

    public ImportParseException(String message) {
        super(message);
    }

    public ImportParseException(String message, Throwable cause) {
        super(message, cause);
    }
}
