package org.lokrusta.prototypes.connect.impl.error;

public class ApiCallDeserializationException extends RuntimeException {

    public ApiCallDeserializationException(Exception e) {
        super(e);
    }
}
