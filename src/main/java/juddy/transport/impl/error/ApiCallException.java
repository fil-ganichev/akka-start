package juddy.transport.impl.error;

public class ApiCallException extends ApiException {

    public ApiCallException(Exception cause) {
        super(cause);
    }

    public ApiCallException(String message) {
        super(message);
    }
}
