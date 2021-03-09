package juddy.transport.impl.kafka.serialize;

public class KafkaSerializationException extends RuntimeException {

    public KafkaSerializationException(Exception e) {
        super(e);
    }
}
