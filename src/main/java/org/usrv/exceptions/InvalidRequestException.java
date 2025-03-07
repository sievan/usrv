package org.usrv.exceptions;

public class InvalidRequestException extends RuntimeException {
    public static final String MISSING_REQUIRED_HOST_MESSAGE = "Missing required Host header";
    public InvalidRequestException(String message) {
        super(message);
    }

    public InvalidRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
