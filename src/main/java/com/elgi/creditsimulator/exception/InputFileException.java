package com.elgi.creditsimulator.exception;

public class InputFileException extends CreditSimulatorException {

    private static final long serialVersionUID = 1L;

    public InputFileException(String message) {
        super(message);
    }

    public InputFileException(String message, Throwable cause) {
        super(message, cause);
    }
}
