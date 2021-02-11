package juddy.transport.impl.error;

public class CallPointNotFoundException extends ApiException {

    public CallPointNotFoundException() {
    }

    public CallPointNotFoundException(String message) {
        super(message);
    }
}
