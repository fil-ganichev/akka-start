package juddy.transport.impl.kafka.producer;

public final class PartitionGenerators {

    private PartitionGenerators() {
    }

    public static PartitionGenerator singlePartitionGenerator() {
        return (topic, key, message) -> 0;
    }

    public static PartitionGenerator defaultKeyPartitionGenerator(int count) {
        return (topic, key, message) -> key.hashCode() % count;
    }
}
