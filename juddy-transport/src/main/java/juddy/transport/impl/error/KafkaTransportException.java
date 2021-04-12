package juddy.transport.impl.error;

public class KafkaTransportException extends RuntimeException {

    public KafkaTransportException(String message) {
        super(message);
    }
}