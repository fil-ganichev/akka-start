package juddy.transport.impl.common;

public final class MessageConstants {

    public static final String API_ENGINE_CONTEXT_NOT_AVAILABLE = "ApiEngineContext is not available. " +
            "Perhaps the instance is not a Spring bean " +
            "or the bean is not initialized";

    public static final String OUTGOING_MESSAGE = "outgoing message";

    public static final String INCOMING_MESSAGE = "incoming message";

    private MessageConstants() {
    }
}
