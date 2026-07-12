package com.elgi.creditsimulator.exception;

public class RemoteServiceException extends RuntimeException {
    private static final long serialVersionUID = 1L;
    public RemoteServiceException(String message) {
        super(message);
    }

    public RemoteServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
