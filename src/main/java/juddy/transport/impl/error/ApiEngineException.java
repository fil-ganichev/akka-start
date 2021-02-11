package juddy.transport.impl.error;

public class ApiEngineException extends RuntimeException {

    public ApiEngineException(Exception e) {
        super(e);
    }

    public ApiEngineException() {
    }

    public ApiEngineException(String errorMessage) {
        super(errorMessage);
    }
}
