package juddy.transport.impl.kafka.producer;

public interface PartitionGenerator {

    int partition(String topic, String key, Object message);
}
