package com.fincopilot.integration;

/**
 * Exception thrown by integration adapters when external API calls fail.
 */
public class IntegrationException extends RuntimeException {

    private final String code;

    public IntegrationException(String code, String message) {
        super(message);
        this.code = code;
    }

    public IntegrationException(String code, String message, Throwable cause) {
        super(message, cause);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}
