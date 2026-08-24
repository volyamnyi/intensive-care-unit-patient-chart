package com.superhumans.mis;

/**
 * Shared error-simulation state for MIS implementations (testing hook).
 * <p>
 * Both {@link MockMisServiceImpl} and {@link WireMockMisServiceImpl} previously
 * duplicated this switch verbatim; centralizing it guarantees that error-mode
 * semantics stay identical regardless of which implementation is active.
 * Modes: none, timeout, not_found, unavailable.
 */
public class MisErrorSimulator {

    private String errorMode = "none";
    private boolean simulateErrors = false;

    public void setErrorMode(String mode) {
        this.errorMode = mode;
        this.simulateErrors = !"none".equals(mode);
    }

    /** Throws/sleeps according to the active mode; a no-op in "none". */
    public void checkErrors() {
        if (simulateErrors) {
            switch (errorMode) {
                case "timeout":
                    try {
                        Thread.sleep(5000);
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    throw new RuntimeException("MIS timeout");
                case "not_found":
                    throw new RuntimeException("Resource not found in MIS");
                case "unavailable":
                    throw new RuntimeException("MIS service unavailable");
                default:
                    // unknown mode — treat as none
            }
        }
    }
}
