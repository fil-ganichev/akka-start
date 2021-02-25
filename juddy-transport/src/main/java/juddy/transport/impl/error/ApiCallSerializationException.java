package juddy.transport.impl.error;

public class ApiCallSerializationException extends RuntimeException {

    public ApiCallSerializationException(Exception e) {
        super(e);
    }
}
