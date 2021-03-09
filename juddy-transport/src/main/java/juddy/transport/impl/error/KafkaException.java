package juddy.transport.impl.error;

public class KafkaException extends RuntimeException {

    public KafkaException(String message) {
        super(message);
    }
}
