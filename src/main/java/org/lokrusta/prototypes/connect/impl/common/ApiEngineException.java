package org.lokrusta.prototypes.connect.impl.common;

public class ApiEngineException extends RuntimeException {

    public ApiEngineException(Exception e) {
        super(e);
    }

    public ApiEngineException() {
        super();
    }

    public ApiEngineException(String errorMessage) {
        super(errorMessage);
    }
}
