package juddy.transport.impl.error;

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
