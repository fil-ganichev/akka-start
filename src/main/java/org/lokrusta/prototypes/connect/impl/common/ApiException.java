package org.lokrusta.prototypes.connect.impl.common;

public class ApiException extends RuntimeException {

    public ApiException(Exception e) {
        super(e);
    }

    public ApiException(String message) {
        super(message);
    }

    protected ApiException() {
    }
}
