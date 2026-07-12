package com.elgi.creditsimulator.exception;

public abstract class CreditSimulatorException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    protected CreditSimulatorException(String message) {
        super(message);
    }

    protected CreditSimulatorException(String message, Throwable cause) {
        super(message, cause);
    }
}
