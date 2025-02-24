package org.usrv.exceptions;

public class RequestParsingException extends RuntimeException {
    public RequestParsingException(String message) {
        super(message);
    }

    public RequestParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}
