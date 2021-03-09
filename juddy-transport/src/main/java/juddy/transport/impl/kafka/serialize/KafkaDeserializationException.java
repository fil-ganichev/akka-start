package juddy.transport.impl.kafka.serialize;

public class KafkaDeserializationException extends RuntimeException {

    public KafkaDeserializationException(Exception e) {
        super(e);
    }
}
