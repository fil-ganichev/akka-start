package juddy.transport.impl.error;

public class ApiCallDeserializationException extends RuntimeException {

    public ApiCallDeserializationException(Exception e) {
        super(e);
    }
}
