package com.elgi.creditsimulator.exception;

public abstract class CreditSimulatorException extends RuntimeException {

    protected CreditSimulatorException(String message) {
        super(message);
    }

    protected CreditSimulatorException(String message, Throwable cause) {
        super(message, cause);
    }
}
